package dewey

import dewey.fsnav.*
import io.github.jwharm.javagi.gio.ListIndexModel
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gio.ListModel
import org.gnome.gobject.GObject
import org.gnome.gtk.*
import java.nio.file.Path
import kotlin.io.path.name
import dewey.fsnav.DirectoryListingResult as DirectoryListing


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
        println("Showing contents for directory: [${pathNavigator.working.path}]")
        when (val cwd = pathNavigator.working) {
            is DirectoryListing.Listing -> {

                // Update current path (top)
                currentPathAndSelectionWidget.updateTargetPath(pathNavigator.working.path) // TODO: move to listing class

                if (cwd.isNotEmpty) {
                    val model = ListIndexModel.newInstance(cwd.count)
                    directoryListWidget.setModelSelectionFactory(model, listViewSelection, itemFactory)

                    // Update current path (top)
                    // TODO: Move up, but for that have to rejiggle dependencies
                    currentPathAndSelectionWidget.updateFocused(cwd.entries[selectedItemIdx].path) // TODO: just use item || entry

                    // Update entry details (bottom)
                    selectedItemDetails.update(cwd.entries[selectedItemIdx])
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

            is DirectoryListing.RestrictedListing -> {
                currentPathAndSelectionWidget.updateTargetPath(pathNavigator.working.path)

                if (cwd.isNotEmpty) {
                    // Update ListView
                    // TODO: Selection model has to be created first, then scrolled to, then remove updates from here
                    //       instead update from selection changed handler
                    val model = ListIndexModel.newInstance(cwd.count)
                    directoryListWidget.setModelSelectionFactory(model, listViewSelection, itemFactoryRestricted)

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

            is DirectoryListing.Error -> {
                when (cwd.err) {
                    DirectoryListing.Error.Type.AccessDenied -> {
                        currentPathAndSelectionWidget.updateTargetPath(pathNavigator.working.path)
                        currentPathAndSelectionWidget.clearFocused()

                        val model = StringList<StringObject>().apply { append("access denied") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)

                        selectedItemDetails.clear()
                    }

                    DirectoryListing.Error.Type.PathNonExistent -> {
                        currentPathAndSelectionWidget.updateTargetPath(pathNavigator.working.path)
                        currentPathAndSelectionWidget.clearFocused()

                        val model = StringList<StringObject>().apply { append("directory does not exist") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)

                        selectedItemDetails.clear()

                    }

                    DirectoryListing.Error.Type.PathNotDirectory -> {
                        // TODO: Is this the best way to handle it? - We should never have to be in this (.PND) code path!
                        currentPathAndSelectionWidget.updateTargetPath(pathNavigator.working.path.parent) /// TODO: Is this the best way?
                        currentPathAndSelectionWidget.updateFocused(pathNavigator.working.path)

                        val model = StringList<StringObject>().apply { append("Given path is not a directory") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)

                        selectedItemDetails.clear()
                    }

                    DirectoryListing.Error.Type.ChangedWhileReading -> TODO()
                }
            }
        }
    }

    private fun openFile(entry: File) {
        println("Opening [${entry.path.name}]")
    }

    private fun openParentDirectory() {
        pathNavigator.working.path.parent?.let {
            todoChangeWorkingDirectory(it)
        }
    }

    private fun reloadDirectory() {
        // TODO: Is this the best way to do it, feels weird?
        todoChangeWorkingDirectory(pathNavigator.working.path)
//        pathNavigator.reload()
//        updateDirectoryList()
    }

    private fun showChangeDirectoryDialog() {
        val dialog = ChangeDirectoryDialog().apply {
            modal = true
            onInputReceive { path ->
                todoChangeWorkingDirectory(path)
            }
            transientFor = this@WidgetDirectoryBrowser.directoryListWidget.root as Window
            present()
        }
    }

    // ------------------------------------------------------------------------ //

    /**
     * Perform default action (open) on selected item.
     */
    private fun openSelectedListViewItem() {
        val workingDir = pathNavigator.working
        if (workingDir !is DirectoryListing.Listing) return
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
     * TODO: Better name
     */
    private fun todoChangeWorkingDirectory(dir: Path) {
        selectionHistory.saveForCurrent()
        pathNavigator.navigateTo(dir)
        updateDirectoryList()
        selectionHistory.restoreForCurrent()
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
            is DirectoryListing.Listing -> {
                val selectedItem = target.entries[selectedItemIdx]
                if (target.isNotEmpty) {
                    currentPathAndSelectionWidget.updateFocused(selectedItem.path)
                    selectedItemDetails.update(selectedItem)
                } else {
                    currentPathAndSelectionWidget.clearFocused()
                    selectedItemDetails.clear()
                }
            }

            is DirectoryListing.RestrictedListing -> {
                val selectedItem = target.entries[selectedItemIdx]
                if (target.isNotEmpty)
                    currentPathAndSelectionWidget.updateFocused(selectedItem.path)
                else
                    currentPathAndSelectionWidget.clearFocused()
                selectedItemDetails.clear()
            }

            is DirectoryListing.Error -> error("UNREACHABLE")
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

    // ----- HELPER INNER CLASSES ------ //


    /**
     * When leaving a directory, save the currently focused entry, ie its path
     *
     * For a given directory, remember the last selected item when the user navigated away from it. Used for easier
     * movement through the file system.
     *
     *  When jumping to distant directory, add paths in-between
     */
    inner class SelectionHistory {

        private val history: MutableMap<Path, Path> = mutableMapOf()

        fun saveForCurrent() {
            if (!pathNavigator.isInitialized) return

            when (val cwd = pathNavigator.working) {
                is DirectoryListing.Listing ->
                    if (cwd.isNotEmpty)
                        update(pathNavigator.working.path, cwd.entries[selectedItemIdx].path)

                is DirectoryListing.RestrictedListing ->
                    if (cwd.isNotEmpty)
                        update(pathNavigator.working.path, cwd.entries[selectedItemIdx].path)

                is DirectoryListing.Error -> {}
            }
        }

        /**
         * When going "up the filesystem tree", child directory we came from should be preselected. Check if this is the
         * case, and if so return that child's Path.
         */
        fun pathOfChildIfNavigatingUp(): Path? =
            pathNavigator.previousWorking?.let { prevWorkingPath ->
                if (pathNavigator.working.path == prevWorkingPath.parent) prevWorkingPath
                else null
            }

        fun restoreForCurrent() {
            // Either, restore what was selected, or if going up to parent directory, select the child we came from.
            val pathToPreselect = getSelected(pathNavigator.working.path) ?: pathOfChildIfNavigatingUp()

            if (pathToPreselect == null) {
                directoryListWidget.focusAndSelectTo(0)
                return
            }

            when (val cwd = pathNavigator.working) {
                is DirectoryListing.Listing -> {
                    val idxSelected = cwd.entries.indexOfFirst { it.path == pathToPreselect }
                    if (idxSelected != -1)
                        directoryListWidget.focusAndSelectTo(idxSelected)
                    else {
                        println("Previously selected entry [${pathToPreselect}] no longer exists in directory [${pathNavigator.working.path}]")
                        directoryListWidget.focusAndSelectTo(0)
                        history.remove(pathNavigator.working.path)
                    }
                }

                is DirectoryListing.RestrictedListing -> {
                    val idxSelected = cwd.entries.indexOfFirst { it.path == pathToPreselect }
                    if (idxSelected != -1)
                        directoryListWidget.focusAndSelectTo(idxSelected)
                    else {
                        println("Previously selected entry [${pathToPreselect}] no longer exists in directory [${pathNavigator.working.path}]")
                        directoryListWidget.focusAndSelectTo(0)
                        history.remove(pathNavigator.working.path)
                    }
                }

                is DirectoryListing.Error -> {}
            }
        }

        private fun update(forDirectory: Path, selection: Path) {
            println("Update: $forDirectory, $selection")
            history[forDirectory] = selection
        }

        private fun getSelected(forDirectory: Path): Path? {
            println("getSelected: $forDirectory")
            return history[forDirectory]
        }

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
                is DirectoryListing.RestrictedListing -> {
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

            val cwd = pathNavigator.working as DirectoryListing.Listing

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