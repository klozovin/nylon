package dewey

import io.github.jwharm.javagi.gio.ListIndexModel
import org.gnome.gdk.Display
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.*
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.*


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
    .symlink {
        color: #2dcee3;
        font-style: italic;
    }
    .unknown {
        color: #000000;
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
        current.label = "${path.pathString}${if (path.parent != null) "/" else ""}"
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
    private val extra = Label("")

    init {
        name = "details"
        append(prefix)
        append(permissions)
        append(owner)
        append(group)
        append(size)
        append(modifiedAt)
        append(extra)
    }

    fun clear() {
        prefix.label = "✕"
        permissions.label = "✕"
        owner.label = "✕"
        group.label = "✕"
        size.label = "✕"
        modifiedAt.label = "✕"
        extra.label = ""
    }

    fun update(path: Path, attributes: PosixFileAttributes) {
        // MAYBE: Clear (reset to default?) before every update?
        prefix.label = when {
            attributes.isSymbolicLink -> "l"
            attributes.isDirectory -> "d"
            attributes.isRegularFile -> "-"
            else -> "-"
        }

        permissions.label = PosixFilePermissions.toString(attributes.permissions())
        owner.label = attributes.owner().name
        group.label = attributes.group().name

        val sizeSuffix = if (!attributes.isDirectory) "B" else ""
        size.label = "${attributes.size()}$sizeSuffix"

        modifiedAt.label = attributes.lastModifiedTime().toString()

        if (attributes.isSymbolicLink)
            extra.label = "⇥ ${path.readSymbolicLink()}"
        else
            extra.label = ""

        // TODO: move somewhere else
        assert(PosixFilePermissions.toString(attributes.permissions()) == posixPermissionsToString(attributes.permissions()))
    }
}


class DirectoryBrowser(path: Path) {

    private lateinit var state: CurrentDirectoryState

    /**
     * For a given directory, remember the last selected item when the user navigated away from it. Used for easier
     * movement through the file system.
     */
    private val directorySelectionHistory: MutableMap<Path, Path> = mutableMapOf()

    private val eventController = EventControllerKey().apply {
        onKeyPressed(::keyPressHandler)
    }

    // Top: Show current directory and selected item
    val currentPathAndSelectionWidget = CurrentPath()

    // Middle: List of directories/files in the current directory
    val directoryListWidget = ListView(null, ItemFactory()).apply {
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
            Gdk.KEY_i -> if (state.selectedItemIdx > 0)
                directoryListWidget.scrollTo(
                    state.selectedItemIdx - 1, setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT), null
                )

            Gdk.KEY_k -> if (state.selectedItemIdx < state.selectionModel.nItems - 1)
                directoryListWidget.scrollTo(
                    state.selectedItemIdx + 1, setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT), null
                )

            Gdk.KEY_F5 -> reloadDirectory()
            Gdk.KEY_F6 -> showChangeDirectoryDialog()
            Gdk.KEY_Left, Gdk.KEY_j -> navigateToParent()
            Gdk.KEY_Right, Gdk.KEY_l -> directoryListWidget.emitActivate(state.selectionModel.selected)
        }
//        println("> Key: [$keyVal]: ${Gdk.keyvalName(keyVal)}, $keyCode, $modifierTypes")
        return false
    }

    private fun showChangeDirectoryDialog() {
        val dialog = ChangeDirectoryDialog().apply {
            modal = true
            onInputReceive { maybePath ->
                println("Maybe we should navigate to...? $maybePath")
                navigateTo(maybePath)
            }
//            transientFor = this@DirectoryBrowser.directoryListWidget.root as Window
            present()
        }
    }

    /**
     * Handle activating an item in the browser.
     */
    private fun activateHandler(idx: Int) {
        // Empty directory, nothing to activate, there's a dummy element in ListView (hacky?)
        if (state.pn.target.isEmpty)
            return

        val activatedPath = state.pn.target.entries[idx].path
        check(activatedPath == state.selectedItem)

        // Skip files when activated
        if (!activatedPath.isDirectory())
            return

        // Skip directories without read permission
        // BUG: Race condition: Permission can be changed after checking it, better to use exception for this.
        //      Maybe still check before atempting?
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
        if (state.pn.target.isEmpty) {
            currentPathAndSelectionWidget.updateFocused(null)
            selectedItemDetails.clear()
            return
        }

        // Update: current path / item name, bottom info
        currentPathAndSelectionWidget.updateFocused(state.selectedItem)
        selectedItemDetails.update(state.selectedItem, state.selectedItemAttributes)
    }

    /**
     * Go to new directory and show its contents.
     */
    private fun navigateTo(target: Path) {
        println("Navigating to directory: ${target}")

        // Still have the old state and Path, save it now
        if (::state.isInitialized && !state.pn.target.isEmpty)
            directorySelectionHistory[state.path] = state.selectedItem
//            directorySelectionHistory.addAll(arrayOf(state.selectedItem, state.path))


        // TODO: State could mark this on startup so that we don't iterate twice over the directory list
        //       -- move it to new FilesystemNavigator class
        state = CurrentDirectoryState(target)
        directoryListWidget.model = state.selectionModel

        // Restore previous selection in this directory.
        directorySelectionHistory.get(state.path)?.let { savedSelection ->
            // There was previously saved selection, but that doesn't mean this Path still exists. It could've been
            // deleted or renamed while we were showing another directory. If the previously saved selection doesn't
            // exist in the current directory listing, delete it.
            val idxSelectedPath = state.pn.target.indexOf(savedSelection)
            if (idxSelectedPath != -1) {
                // Found it, move the ListView selection *and* focus to it.
                directoryListWidget.scrollTo(
                    idxSelectedPath,
                    setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT),
                    null
                )
            } else {
                // Item was saved, but it's no longer in the directory. Clear it.
                directorySelectionHistory.remove(savedSelection)
            }
        }

        currentPathAndSelectionWidget.updateCurrent(state.path)
        if (!state.pn.target.isEmpty) {
            // Current directory is NOT empty
            currentPathAndSelectionWidget.updateFocused(state.selectedItem)
            selectedItemDetails.update(state.selectedItem, state.selectedItemAttributes)
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
     * Reload current directory.
     */
    private fun reloadDirectory() {
        println("Reloading: ${state.path}")
        navigateTo(state.path) // Somehow feels ... wrong?
    }

    /**
     * Keep the state related to single directory view in one place. Don't mutate, recreate.
     */
    inner class CurrentDirectoryState(val path: Path) {

        val pn = PathNavigator().apply { navigateTo(path) }

        val dirListModel = ListIndexModel.newInstance(if (!pn.target.isEmpty) pn.target.count else 1)

        val selectionModel = SingleSelection(dirListModel).apply {
            onSelectionChanged { _, _ -> selectionChangedHandler() }
        }

        val selectedItemIdx: Int
            get() = selectionModel.selected

        val selectedItem: Path
            get() = pn.target.entries[selectionModel.selected].path

        val selectedItemAttributes: PosixFileAttributes
            get() = pn.target.entries[selectionModel.selected].attributes

        init {
            println("Creating [CurrentDirectoryState]")
        }
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
            val listItemLabel = listItem.child as Label

            val item = listItem.item as ListIndexModel.ListIndex

            // Clear all CSS classes from the Label (because virtualized ListView)
            listItemLabel.cssClasses = emptyArray()

            // Showing an empty directory
            if (state.pn.target.isEmpty) {
                check(state.dirListModel.nItems == 1) { "On empty directory ListView should have only one element inside." }
                listItemLabel.label = "<< empty >>"
                listItemLabel.addCssClass("empty")
                return
            }

            check(item.index == listItem.position)

            val itemEntry = state.pn.target.entries[listItem.position]
            val entryName = itemEntry.path.name
            when {
                itemEntry.isSymbolicLink -> {
                    if (itemEntry.isDirectory)
                        listItemLabel.label = "[[⇥$entryName]]"
                    else
                        listItemLabel.label = "⇥ $entryName"
                    listItemLabel.addCssClass("symlink")
                }

                itemEntry.isDirectory -> {
                    listItemLabel.addCssClass("directory")
                    listItemLabel.label = "[[$entryName]]"
                }

                itemEntry.isRegularFile -> {
                    listItemLabel.label = entryName
                    listItemLabel.addCssClass("file")
                }

                else -> {
                    listItemLabel.label = entryName
                    listItemLabel.addCssClass("unknown")
                }
            }
            check(listItemLabel.cssClasses.size <= 1) { "Can't have more than one class set" }
        }
    }
}

class ChangeDirectoryDialog : Window() {

    private val directoryPathInput = Entry().apply {

        placeholderText = "/path/to/cd/into"

        // Close everything on Escape
        addController(EventControllerKey().apply {
            onKeyPressed { keyVal, _, _ ->
                when (keyVal) {
                    Gdk.KEY_Escape -> close()
                }
                return@onKeyPressed false
            }
        })
    }

    fun onInputReceive(callback: (Path) -> Unit) {
        directoryPathInput.onActivate {
            val entryPath = Path(directoryPathInput.text)
            if (Files.exists(entryPath, LinkOption.NOFOLLOW_LINKS)) {
                // TODO: BUG: Race condition, should use exceptions for this
                close()
                callback(entryPath)
            } else
                println("Error: Tried to cd into non existent directory.")
        }
    }

    init {
//        directoryPathInput.onActivate {
//            println("activated")
//            close()
//        }
        child = directoryPathInput
    }
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
        val homeDirectory = Path(System.getProperty("user.home"))
        val startupDirectory = args.getOrNull(0)?.let { Path(it) } ?: homeDirectory

        val browser = DirectoryBrowser(startupDirectory)
        window.child = browser.boxWidget
        window.present()
    }

    app.onShutdown {
        println("App shutting down")
    }

    app.run(args)
}