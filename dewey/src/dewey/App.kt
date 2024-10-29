package dewey

import org.gnome.gdk.Display
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.*
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.*
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.*


val homeDirectory = Path(System.getProperty("user.home"))

val styleCss = """
    window {
        font-size: 18px;
    }
    .directory {
        color: #ff0000;
        font-weight: bold;
    }
    .file {
        color: #0000ff;
    }
    .empty {
        color: #000000;
        font-style: italic;
    }
    
    box.cwd {
        color: #00ff00;
        font-weight: bold;
    }
    box.focused {
        color: #000000;
        font-style: italic;
    }
"""


class CurrentPath : Box(Orientation.HORIZONTAL, 4) {
    val current = Label("✕").apply { addCssClass("cwd") }
    val separator = Label(" → ").apply { addCssClass("separator") }
    val focused = Label("✕").apply { addCssClass("focused") }

    init {
        append(current)
        append(separator)
        append(focused)
    }

    fun updateCurrent(path: Path) {
        // Don't show double "//" when showing the root filesystem.
        current.label = "${path.pathString}${if (path.parent != null) "/" else "✕"}"
    }

    fun updateFocused(path: Path?) {
        // path = null for empty directories, so there's nothing to focus on
        focused.label = path?.fileName?.pathString ?: "✕"
    }
}


class Details : Box(Orientation.HORIZONTAL, 8) {
    private val prefix = Label("✕").apply { addCssClass("prefix") }
    private val permissions = Label("✕").apply { addCssClass("permissions") }
    private val owner = Label("✕").apply { addCssClass("owner") }
    private val group = Label("✕").apply { addCssClass("group") }
    private val size = Label("✕").apply { addCssClass("size") }
    private val modifiedAt = Label("✕").apply { addCssClass("modified-at") }

    init {
        name = "details"
        append(prefix)
        append(permissions)
        append(owner)
        append(group)
        append(size)
        append(modifiedAt)
    }

    fun clear() {
        prefix.label = "✕"
        permissions.label = "✕"
        owner.label = "✕"
        group.label = "✕"
        size.label = "✕"
        modifiedAt.label = "✕"
    }

    fun update(path: Path) {
        val attributes = path.readAttributes<PosixFileAttributes>()

        prefix.label = when {
            attributes.isDirectory -> "d"
            attributes.isRegularFile -> "-"
            attributes.isSymbolicLink -> "l"
            else -> "-"
        }

        permissions.label = PosixFilePermissions.toString(attributes.permissions())
        owner.label = attributes.owner().name
        group.label = attributes.group().name

        val sizeSuffix = if (!attributes.isDirectory) "B" else ""
        size.label = "${attributes.size()}$sizeSuffix"

        modifiedAt.label = attributes.lastModifiedTime().toString()

        // TODO: move somewhere else
        assert(PosixFilePermissions.toString(attributes.permissions()) == posixPermissionsToString(attributes.permissions()))
    }
}


class DirectoryBrowser(path: Path) {

    private lateinit var state: CurrentDirectoryState
    private val itemFactory = ItemFactory()

    private val eventController = EventControllerKey().apply {
        onKeyPressed(::keyPressHandler)
    }

    // Top: Show current directory and selected item
    val currentPathAndSelectionWidget = CurrentPath()

    // Middle: List of directories/files in the current directory
    val directoryListWidget = ListView(null, itemFactory).apply {
        onActivate(::activateHandler)
        addController(eventController)
    }

    // Scrolll container for the listing
    val scrolledWidget = ScrolledWindow().apply {
        child = directoryListWidget
        vexpand = true
    }

    // Bottom: Details about the selected item (directory/file)
//    val selectedItemDetails = Label("More details come here...")
    val selectedItemDetails = Details()

    // Main parent widget, contains everything else
    val boxWidget = Box(Orientation.VERTICAL, 8).apply {
        vexpand = true
        append(currentPathAndSelectionWidget)
        append(scrolledWidget)
        append(selectedItemDetails)
    }

    init {
        navigateTo(path)
    }

    /**
     * Handle keyboard shortcuts in browser.
     */
    private fun keyPressHandler(keyVal: Int, keyCode: Int, modifierTypes: MutableSet<ModifierType>?): Boolean {
        when (keyVal) {
            Gdk.KEY_Left, Gdk.KEY_j -> navigateToParent()
            Gdk.KEY_Right, Gdk.KEY_l -> directoryListWidget.emitActivate(state.selectionModel.selected)
        }
//        println("> Key: [$keyVal]: ${Gdk.keyvalName(keyVal)}, $keyCode, $modifierTypes")
        return false
    }

    /**
     * Handle activating an item in the browser.
     */
    private fun activateHandler(idx: Int) {
        // Empty directory, nothing to activate, there's a dummy element in ListView (hacky?)
        if (state.isEmpty)
            return

        val activatedPath = state.dirList[idx]

        // Skip files when activated
        if (!activatedPath.isDirectory())
            return

        // Skip directories without read permission
        // BUG: Race condition: Permission can be changed after checking it, better to use exception for this.
        if (!activatedPath.isReadable())
            return

        println("> Activated: [${activatedPath}]")
        navigateTo(activatedPath)
    }

    /**
     * Updates the UI when the selection changes.
     *
     * Called by:
     * - SelectionModel selection-changed signal.
     * - DirectoryBrowser, when navigating to a directory
     */
    private fun selectionChangedHandler() {
        // Check if inside empty-directory: ListView is not empty, but that item doesn't represent a real dir/file.
        if (state.dirList.isEmpty()) {
            currentPathAndSelectionWidget.updateFocused(null)
            selectedItemDetails.clear()
            return
        }

        // Update: current path / item name, bottom info
        val selectedItem = state.dirList[state.selectionModel.selected]
        currentPathAndSelectionWidget.updateFocused(selectedItem)
        selectedItemDetails.update(selectedItem)
    }

    /**
     * Go to new directory and show its contents.
     */
    private fun navigateTo(target: Path) {
        println("Navigating to directory: ${target}")
        state = CurrentDirectoryState(target)
        directoryListWidget.model = state.selectionModel
        currentPathAndSelectionWidget.updateCurrent(state.path)
        if (!state.isEmpty) {
            // Current directory is NOT empty
            val selectedItem = state.dirList[state.selectionModel.selected]
            currentPathAndSelectionWidget.updateFocused(selectedItem)
            selectedItemDetails.update(selectedItem)
        } else {
            // Current directory empty: clear info at bottom, and selected in path on top
            currentPathAndSelectionWidget.updateFocused(null)
            selectedItemDetails.clear()
        }
    }

    /**
     * Go up in directory hierarchy.
     */
    private fun navigateToParent() {
        val parent = state.path.parent ?: return // Handle when showing root
        navigateTo(parent)
    }

    /**
     * Keep the state related to single directory view in one place. Don't mutate, recreate.
     */
    inner class CurrentDirectoryState(val path: Path) {
        val dirList = path.listDirectoryEntries().sortedByDescending { it.isDirectory() }
        val isEmpty = dirList.isEmpty()

        // Handle empty directories by adding a dummy item to ListView model
        val dirListModel =
            if (!isEmpty) StringList(dirList.map { pathToString(it) }.toTypedArray())
            else StringList(arrayOf("<< empty folder >>"))

        val selectionModel = SingleSelection(dirListModel).apply {
            onSelectionChanged { _, _ -> selectionChangedHandler() }
        }

        private fun pathToString(path: Path): String =
            if (path.isDirectory()) // TODO (perf): read attributes in one place
                "[[${path.fileName}]]"
            else
                path.fileName.toString()
    }


    /**
     * Creates and updates rows in virtualized ListView control.
     */
    inner class ItemFactory : SignalListItemFactory() {
        init {
            println(">>> New list item factory factoried <<<")
            onSetup { setup(it as ListItem) }
            onBind { bind(it as ListItem) }
        }

        private fun setup(listItem: ListItem) {
            val label = Label("✕").apply { halign = Align.START }
            listItem.child = label
        }

        private fun bind(listItem: ListItem) {
            val label = listItem.child as Label
            val item = listItem.item as StringObject

            label.label = item.string
            // Clear all CSS classes from the Label widget because the widgets get reused by ListView.
            label.cssClasses = emptyArray()

            if (!state.isEmpty) {
                // Showing a non-empty directory
                if (state.dirList[listItem.position].isDirectory())
                    label.addCssClass("directory")
                else
                    label.addCssClass("file")
            } else {
                // TODO: Maybe remove this check?
                check(state.dirListModel.nItems == 1) { "On empty directory ListView should have only one element inside." }
                label.addCssClass("empty")
            }
            check(label.cssClasses.size <= 1) { "Can't have more than one class set" }
        }
    }
}


fun posixPermissionsToString(permissions: Set<PosixFilePermission>): String {
    val permissionsOrdered = arrayOf(
        OWNER_READ,
        OWNER_WRITE,
        OWNER_EXECUTE,

        GROUP_READ,
        GROUP_WRITE,
        GROUP_EXECUTE,

        OTHERS_READ,
        OTHERS_WRITE,
        OTHERS_EXECUTE,
    )
    val permissionChars = charArrayOf('r', 'w', 'x')
    val permissionStringBuilder = StringBuilder(9)

    for ((idx, permission) in permissionsOrdered.withIndex())
        permissionStringBuilder.insert(
            idx,
            if (permission in permissions) permissionChars[idx % 3] else '-'
        )

    return permissionStringBuilder.toString()
}


fun main(args: Array<String>) {
    println("Running with arguments: ${args.contentToString()}")
    val app = Application("com.sklogw.nylon.Dewey", ApplicationFlags.DEFAULT_FLAGS)
    val css = CssProvider().apply {
        onParsingError { section, error -> println("CSS parsing error: $section, ${error.readMessage()}") }
        loadFromString(styleCss)
    }

    app.onStartup {
        println("App startup")
    }

    app.onActivate {
        println("App activating")
        StyleContext.addProviderForDisplay(Display.getDefault(), css, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)
        val window = ApplicationWindow(app)

        // Use home directory if nothing passed on the command line
        val startupDirectory = args.getOrNull(0)?.let { Path(it) } ?: homeDirectory

        val browser = DirectoryBrowser(startupDirectory)
        window.child = browser.boxWidget
        window.present()
    }

    app.onShutdown {
        println("App shutting down")
    }

    app.run(args)

    homeDirectory.getPosixFilePermissions()
}