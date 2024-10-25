package dewey

import org.gnome.gdk.Display
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.*
import java.nio.file.Path

import kotlin.io.path.*


val homeDirectory = Path(System.getProperty("user.home"))

val styleCss = """
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
    val current = Label("").apply { addCssClass("cwd") }
    val separator = Label(" → ").apply { addCssClass("separator") }
    val focused = Label("").apply { addCssClass("focused") }

    init {
        halign = Align.START
        append(current)
        append(separator)
        append(focused)
    }

    fun updateCurrent(path: Path) {
        // Don't show double "//" when showing the root filesystem.
        current.label = "${path.pathString}${if (path.parent != null) "/" else ""}"
    }

    fun updateFocused(path: Path?) {
        // Empty directories
        focused.label = path?.fileName?.pathString ?: ""
    }
}

class DirectoryBrowser(path: Path) {

    private var state = State(path)
    private val itemFactory = ItemFactory()

    private val eventController = EventControllerKey().apply {
        onKeyPressed(::keyPressHandler)
    }

    // Top: Show current directory and selected item
    val currentPathAndSelectionWidget = CurrentPath().apply {
        updateCurrent(state.path)
        updateFocused(state.dirList[state.selectionModel.selected]) // BUG?: What if we start in empty directory?
    }

    // Middle: List of directories/files in the current directory
    val directoryListWidget = ListView(state.selectionModel, itemFactory).apply {
        onActivate(::activateHandler)
        addController(eventController)
    }
    // Scrolll container for the listing
    val scrolledWidget = ScrolledWindow().apply {
        child = directoryListWidget
        vexpand = true
    }

    // Bottom: Details about the selected item (directory/file)
    val selectedItemDetails = Label("More details come here...").apply { halign = Align.START }

    // Main parent widget, contains everything else
    val boxWidget = Box(Orientation.VERTICAL, 8).apply {
        append(currentPathAndSelectionWidget)
        append(scrolledWidget)
        append(selectedItemDetails)
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
        // BUG: Race condition: permission can be changed after checking it, better to use exception for this.
        if (!activatedPath.isReadable())
            return

        println("> Activated: [${activatedPath}]")
        navigateTo(activatedPath)
    }

    /**
     * Called when currently selected item in directory browser changes. Update UI accordingly.
     */
    private fun selectionChangedHandler(i: Int, i1: Int) {
        val selected = state.dirList[state.selectionModel.selected]
        currentPathAndSelectionWidget.updateFocused(selected)
    }

    /**
     * Go to new directory and show its contents.
     */
    private fun navigateTo(target: Path) {
        println("Navigating to directory: ${target}")
        state = State(target)
        directoryListWidget.model = state.selectionModel
        currentPathAndSelectionWidget.updateCurrent(state.path)

        // BUG: empty directory?
        if (!state.isEmpty)
            currentPathAndSelectionWidget.updateFocused(state.dirList[state.selectionModel.selected])
        else
            currentPathAndSelectionWidget.updateFocused(null)
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
    inner class State(val path: Path) {
        val dirList = path.listDirectoryEntries().sortedByDescending { it.isDirectory() }

        val isEmpty = dirList.isEmpty()

        // Handle empty directories by adding a dummy item to ListView model
        val dirListModel =
            if (!isEmpty) StringList(dirList.map { pathToString(it) }.toTypedArray())
            else StringList(arrayOf("<< empty folder >>"))

        val selectionModel = SingleSelection(dirListModel).apply {
            onSelectionChanged(::selectionChangedHandler)
        }

        private fun pathToString(path: Path): String =
            if (path.isDirectory())
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
            listItem.child = Label("")
            listItem.child.halign = Align.START
            println("Creating a new label")
        }

        private fun bind(listItem: ListItem) {
            val label = listItem.child as Label
            val item = listItem.item as StringObject

            label.label = item.string
            check(label.cssClasses.size <= 1) { "Can't have more than one class set" }

            if (!state.isEmpty) {
                // Showing a non-empty directory
                if (state.dirList[listItem.position].isDirectory())
                    label.addCssClass("directory")
                else
                    label.addCssClass("file")
            } else {
                check(state.dirListModel.nItems == 1) { "On empty directory ListView should have only one element inside." }
                label.addCssClass("empty")
            }
//            println("Postion: ${listItem.position}")
        }
    }
}


fun main(args: Array<String>) {
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
        val browser = DirectoryBrowser(homeDirectory)
        window.child = browser.boxWidget
        window.present()
    }

    app.onShutdown {
        println("App shutting down")
    }

    app.run(args)
}