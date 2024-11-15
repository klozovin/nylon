package dewey

import io.github.jwharm.javagi.gio.ListIndexModel
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gio.ListModel
import org.gnome.gobject.GObject
import org.gnome.gtk.*
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.pathString


class WidgetDirectoryBrowser(path: Path) {

    val pathNavigator = FilesystemNavigator()
    val selectionHistory = SelectionHistory()

    private val eventController = EventControllerKey().apply {
        onKeyPressed(::keyPressHandler)
    }

    //
    // TOP: Show current directory and selected item
    //
    val currentPathAndSelectionWidget = WidgetCurrentPath()

    //
    // MIDDLE: List of directories/files in the current directory
    //
    val itemFactory = ItemFactoryRegularDirectory()
    val itemFactoryEmpty = ItemFactoryEmptyDirectory()
    val itemFactoryRestricted = ItemFactoryRestrictedDirectory()

    val listViewSelection = SingleSelection<ListIndexModel.ListIndex>().apply {
        canUnselect = false
        onSelectionChanged(::selectionChangedHandler)
    }
    val selectedItemIdx: Int
        get() {
            require(directoryListWidget.model == listViewSelection)
            return listViewSelection.selectedItem.index
        }
    val directoryListWidget = ListView(null, null).apply {
        addController(eventController)
        onActivate(::activateHandler)
    }

    // Scroll container for the listing
    val scrolledWidget = ScrolledWindow().apply {
        child = directoryListWidget
        vexpand = true
    }

    //
    // BOTTOM: Details about the selected item (directory/file)
    //
    val selectedItemDetails = WidgetDetails()

    //
    // Main parent widget, contains everything else
    //
    val boxWidget = Box(Orientation.VERTICAL, 8).apply {
        vexpand = true
        append(currentPathAndSelectionWidget)
        append(scrolledWidget)
        append(selectedItemDetails)
    }

    init {
        todoChangeWorkingDirectory(path)
    }

    private fun updateDirectoryList() {
        println("Showing contents for directory: [${pathNavigator.workingPath.pathString}]")
        when (val cwd = pathNavigator.working) {
            is DirectoryListingResult.Listing -> {

                // Update ListView
                // TODO: Delete?
//                listViewState.update(target)

                // Update current path (top)
                currentPathAndSelectionWidget.updateTargetPath(pathNavigator.workingPath) // TODO: move to listing class

                if (cwd.isNotEmpty) {
                    val model = ListIndexModel.newInstance(cwd.count)
                    directoryListWidget.setModelSelectionFactory(model, listViewSelection, itemFactory)

                    // Update current path (top)
                    // TODO: Move up, but for that have to rejiggle dependencies
                    currentPathAndSelectionWidget.updateFocused(cwd.entries[selectedItemIdx].path) // TODO: just use item || entry

                    // Update entry details (bottom)
                    selectedItemDetails.update(cwd.entries[selectedItemIdx])

                    val previouslySelected = selectionHistory.getSelected(forDirectory = pathNavigator.workingPath)
                    if (previouslySelected != null) {
                        val idxSelected = cwd.entries.indexOfFirst { it.path == previouslySelected }
                        if (idxSelected != -1)
                            directoryListWidget.focusAndSelectTo(idxSelected)
                        else
                            println("Previously selected entry no longer exists in this directory")
                    } else {
                        directoryListWidget.focusAndSelectTo(0)
                    }
                }

                // Showing an empty directory
                else {
                    // Update current path (top): CWD path remains the same, target gets cleared in an empty dir.
                    currentPathAndSelectionWidget.clearFocused()

                    // Update entry listing (middle): set to emtpy item factory
                    val model = StringList<StringObject>().apply { append("empty directory") }
                    val selection = NoSelection<GObject>(model)
                    directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)

                    // Update entry details (bottom): nothing to show, clear it
                    selectedItemDetails.clear()
                }
            }

            is DirectoryListingResult.RestrictedListing -> {
                currentPathAndSelectionWidget.updateTargetPath(pathNavigator.workingPath)

                if (cwd.isNotEmpty) {
                    // Update ListView
                    // TODO: Selection model has to be created first, then scrolled to, then remove updates from here
                    //       instead update from selection changed handler
                    val model = ListIndexModel.newInstance(cwd.count)
                    directoryListWidget.setModelSelectionFactory(model, listViewSelection, itemFactoryRestricted)

                    // MUST scrollTo only after setting model/factory (breaks if going from a restricted to regular directory)
                    directoryListWidget.focusAndSelectTo(0) // TODO: Remove, set from history

                    // Update current path (top)
                    currentPathAndSelectionWidget.updateFocused(cwd.entries[listViewSelection.selected].path)

                    // Update entry details (bottom): can't show anything in restricted directory
                    selectedItemDetails.clear()
                }
                // Showing an empty directory
                else {
                    // Update current path (top)
                    currentPathAndSelectionWidget.clearFocused()

                    val model = StringList<StringObject>().apply { append("empty restricted directory") }
                    val selection = NoSelection<GObject>(model)
                    directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)

                    // Update entry details (bottom): clear
                    selectedItemDetails.clear()
                }
            }

            is DirectoryListingResult.Error -> {
                when (cwd.err) {
                    DirectoryListingResult.Error.Type.AccessDenied -> {
                        currentPathAndSelectionWidget.updateTargetPath(pathNavigator.workingPath)
                        currentPathAndSelectionWidget.clearFocused()

                        val model = StringList<StringObject>().apply { append("access denied") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)

                        selectedItemDetails.clear()
                    }

                    DirectoryListingResult.Error.Type.PathNonExistent -> {
                        currentPathAndSelectionWidget.updateTargetPath(pathNavigator.workingPath)
                        currentPathAndSelectionWidget.clearFocused()

                        val model = StringList<StringObject>().apply { append("directory does not exist") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)

                        selectedItemDetails.clear()

                    }

                    DirectoryListingResult.Error.Type.PathNotDirectory -> TODO()
                    DirectoryListingResult.Error.Type.ChangedWhileReading -> TODO()
                }
            }
        }
    }

    private fun openFile(entry: File) {
        println("Opening [${entry.path.name}]")
    }

    private fun openParentDirectory() {
        pathNavigator.workingPath.parent?.let {
            todoChangeWorkingDirectory(it)
        }
    }

    private fun reloadDirectory() {
        pathNavigator.reload()
        updateDirectoryList()
    }

    private fun showChangeDirectoryDialog() {
        val dialog = ChangeDirectoryDialog().apply {
            modal = true
            onInputReceive { maybePath ->
                println("Maybe we should navigate to...? $maybePath")
                pathNavigator.navigateTo(maybePath)
                updateDirectoryList()
            }
            transientFor = this@WidgetDirectoryBrowser.directoryListWidget.root as Window
        }
        dialog.present()
    }

    // ------------------------------------------------------------------------ //

    /**
     * Perform default action (open) on selected item.
     */
    private fun openSelectedListViewItem() {
        val workingDir = pathNavigator.working
        if (workingDir !is DirectoryListingResult.Listing) return
        if (workingDir.isEmpty) return

        when (val selectedItem = workingDir.entries[selectedItemIdx]) {
            is Directory -> todoChangeWorkingDirectory(selectedItem.path)
            is Symlink -> {
                if (selectedItem.linksToType() == "d")
                    todoChangeWorkingDirectory(selectedItem.path)
            }

            is File -> println("Not yet implemented...")
            is Other -> println("Not yet implemented...")
        }
    }

    /**
     * Centralise in one place
     */
    private fun todoChangeWorkingDirectory(dir: Path) {

        // TODO: Works only for changing into parent or immediate child.
        // Save selection for the old directory listing
        if (pathNavigator.isInitialized) when (val cwd = pathNavigator.working) {
            is DirectoryListingResult.Listing -> {
                selectionHistory.update(
                    forDirectory = pathNavigator.workingPath,
                    selection = cwd.entries[selectedItemIdx].path
                )
            }

            is DirectoryListingResult.RestrictedListing ->
                selectionHistory.update(
                    forDirectory = pathNavigator.workingPath,
                    selection = cwd.entries[selectedItemIdx].path
                )

            else -> {}
        }

        pathNavigator.navigateTo(dir)
        updateDirectoryList()
        // Restore selection for this new directory listing
    }

    private fun keyPressHandler(keyVal: Int, keyCode: Int, modifierTypes: MutableSet<ModifierType>?): Boolean {
        when (keyVal) {
            Gdk.KEY_i -> selectionUp()
            Gdk.KEY_k -> selectionDown()

            Gdk.KEY_F5 -> reloadDirectory()
            Gdk.KEY_F6 -> showChangeDirectoryDialog()

            Gdk.KEY_Left, Gdk.KEY_j -> openParentDirectory()
            Gdk.KEY_Right, Gdk.KEY_l -> openSelectedListViewItem()
        }
        return false
    }

    private fun activateHandler(activatedIdx: Int) =
        openSelectedListViewItem()

    private fun selectionChangedHandler(position: Int, items: Int) {
        when (val target = pathNavigator.working) {
            is DirectoryListingResult.Listing -> {
                val selectedItem = target.entries[selectedItemIdx]
                if (target.isNotEmpty) {
                    currentPathAndSelectionWidget.updateFocused(selectedItem.path)
                    selectedItemDetails.update(selectedItem)
                } else {
                    currentPathAndSelectionWidget.clearFocused()
                    selectedItemDetails.clear()
                }
            }

            is DirectoryListingResult.RestrictedListing -> {
                val selectedItem = target.entries[selectedItemIdx]
                if (target.isNotEmpty)
                    currentPathAndSelectionWidget.updateFocused(selectedItem.path)
                else
                    currentPathAndSelectionWidget.clearFocused()
                selectedItemDetails.clear()
            }

            is DirectoryListingResult.Error -> error("UNREACHABLE")
        }
    }

    private fun selectionUp() {
        if (directoryListWidget.model is NoSelection) return
        if (selectedItemIdx > 0) directoryListWidget.focusAndSelectTo(selectedItemIdx - 1)
    }

    private fun selectionDown() {
        if (directoryListWidget.model is NoSelection) return
        if (selectedItemIdx < listViewSelection.nItems - 1) directoryListWidget.focusAndSelectTo(selectedItemIdx + 1)
    }

    abstract class BaseItemFactory : SignalListItemFactory() {
        init {
            onSetup { setup(it as ListItem) }
            onBind { bind(it as ListItem) }
        }

        open fun setup(listItem: ListItem) {
            listItem.child = Label("✕").apply { halign = Align.START }
        }

        abstract fun bind(listItem: ListItem)

    }

    /**
     * Used when there's only one item in the directory listing, but that item is not a directory entry, instead it's
     * a marker: empty directory, access denied, etc.
     */
    inner class ItemFactoryEmptyDirectory : BaseItemFactory() {
        override fun bind(listItem: ListItem) {
            val child = listItem.child as Label
            val item = listItem.item as StringObject
            child.cssClasses = emptyArray()
            child.label = "<< ${item.string}>> "
            check(child.cssClasses.size <= 1) { "Can't have more than one class set" }
        }
    }

    /**
     * Showing a directory with [rw-] permissions. Can only show paths, no entry details.
     */
    inner class ItemFactoryRestrictedDirectory : BaseItemFactory() {
        override fun bind(listItem: ListItem) {
            val listItemLabel = listItem.child as Label
            listItemLabel.cssClasses = emptyArray()
            when (val target = pathNavigator.working) {
                is DirectoryListingResult.RestrictedListing -> {
                    val itemEntry = target.entries[listItem.position]
                    listItemLabel.apply {
                        label = "% ${itemEntry.path.name}"
                        addCssClass("restricted")
                    }
                }

                else -> error("Should be unreachable")
            }
            check(listItemLabel.cssClasses.size <= 1) { "Can't have more than one class set" }
        }
    }

    /**
     * Showing a regular directory with [rwx] permissions. Can get full details about entries.
     */
    inner class ItemFactoryRegularDirectory : BaseItemFactory() {
        override fun bind(listItem: ListItem) {
            val listItemLabel = listItem.child as Label
            val item = listItem.item as ListIndexModel.ListIndex

            // Clear all CSS classes from the Label (because virtualized ListView)
            listItemLabel.cssClasses = emptyArray()

            val cwd = pathNavigator.working as DirectoryListingResult.Listing

            check(item.index == listItem.position)
            when (val itemEntry = cwd.entries[listItem.position]) {
                is Symlink -> {
                    listItemLabel.label = when (itemEntry.linksToType()) {
                        "d" -> "[[⇥ ${itemEntry.path.name}]]"
                        else -> "⇥ ${itemEntry.path.name}"
                    }
                    listItemLabel.addCssClass("symlink")

                }

                is Directory -> {
                    listItemLabel.apply {
                        label = "[[${itemEntry.path.name}]]"
                        addCssClass("directory")
                    }

                }

                is File -> {
                    listItemLabel.apply {
                        label = itemEntry.path.name
                        addCssClass("file")
                    }

                }

                is Other -> {
                    listItemLabel.apply {
                        label = itemEntry.path.name
                        addCssClass("unknown")
                    }

                }
            }
            check(listItemLabel.cssClasses.size <= 1) { "Can't have more than one class set" }
        }
    }

    companion object {
        /**
         * Have to do it by setting to null first, otherwise there's a race condition.
         */
        fun <T : GObject> ListView.setSelectionAndFactory(selection: SelectionModel<T>, factory: ListItemFactory) {
            this.model = null
            this.factory = null
            this.model = selection
            this.factory = factory
        }

        fun <T : GObject> ListView.setModelSelectionFactory(
            model: ListModel<T>,
            selection: SelectionModel<T>,
            factory: ListItemFactory
        ) {
            this.model = null
            this.factory = null
            when (selection) {
                is SingleSelection -> selection.model = model
                is NoSelection -> selection.model = model
                else -> error("UNREACHABLE")
            }
            this.model = selection
            this.factory = factory
        }

        fun ListView.focusAndSelectTo(idx: Int) {
            scrollTo(idx, setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT), null)
        }
    }
}
