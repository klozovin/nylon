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

object DirectoryBrowserState {
    var cwdListing: DirectoryListing by SubjectDelegate()
    var selectedItem: BaseDirectoryEntry? by NullableSubjectDelegate()
}


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
                if (cwd.isNotEmpty) {
                    val model = ListIndexModel.newInstance(cwd.count)
                    directoryListWidget.setModelSelectionFactory(model, listViewSelection, itemFactory)
                    DirectoryBrowserState.selectedItem = cwd.entries[selectedItemIdx]
                }

                // Showing an empty directory
                else {
                    // Update entry listing (middle): set to empty item factory
                    val model = StringList<StringObject>().apply { append("empty directory") }
                    val selection = NoSelection<GObject>(model)
                    directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)
                    DirectoryBrowserState.selectedItem = null
                }
            }

            is DirectoryListing.RestrictedListing -> {
                if (cwd.isNotEmpty) {
                    // Update ListView
                    // TODO: Selection model has to be created first, then scrolled to, then remove updates from here
                    //       instead update from selection changed handler
                    val model = ListIndexModel.newInstance(cwd.count)
                    directoryListWidget.setModelSelectionFactory(model, listViewSelection, itemFactoryRestricted)
                    DirectoryBrowserState.selectedItem = cwd.entries[selectedItemIdx] // TODO: Maybe move at the end?
                }
                // Showing an empty directory
                else {
                    val model = StringList<StringObject>().apply { append("empty restricted directory") }
                    val selection = NoSelection<GObject>(model)
                    directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)
                    DirectoryBrowserState.selectedItem = null
                }
            }

            is DirectoryListing.Error -> {
                DirectoryBrowserState.selectedItem = null // TODO: Move out of here
                when (cwd.err) {
                    DirectoryListing.Error.Type.AccessDenied -> {
                        val model = StringList<StringObject>().apply { append("access denied") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)
                    }

                    DirectoryListing.Error.Type.PathNonExistent -> {
                        val model = StringList<StringObject>().apply { append("directory does not exist") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)
                    }

                    DirectoryListing.Error.Type.PathNotDirectory -> {
                        // TODO: Is this the best way to handle it? - We should never have to be in this (.PND) code path!

                        val model = StringList<StringObject>().apply { append("Given path is not a directory") }
                        val selection = NoSelection<GObject>(model)
                        directoryListWidget.setSelectionAndFactory(selection, itemFactoryEmpty)
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
        ChangeDirectoryDialog().apply {
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
        DirectoryBrowserState.cwdListing = pathNavigator.working
        // TODO: set selected item observable here
//        println(selectedItemIdx)
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
        // TODO: Entire function should be just one line getting selected item
        when (val target = pathNavigator.working) {
            is DirectoryListing.Listing -> {
                val selectedItem = target.entries[selectedItemIdx]
                if (target.isNotEmpty) {
                    DirectoryBrowserState.selectedItem = selectedItem
                } else {
                    DirectoryBrowserState.selectedItem = null
                }
            }

            is DirectoryListing.RestrictedListing -> {
                val selectedItem = target.entries[selectedItemIdx]
                if (target.isNotEmpty) {
                    DirectoryBrowserState.selectedItem = selectedItem
                } else {
//                    currentPathAndSelectionWidget.clearFocused()
                }
            }

            is DirectoryListing.Error -> unreachable()
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
     * When navigating the filesystem, preselect entries for easier navigation. Preselect either:
     *
     * 1) if navigating to parent directory, child we came from
     * 2) last selected entry
     *
     */
    inner class SelectionHistory {

        private val history: MutableMap<Path, Path> = mutableMapOf()

        /**
         * Save currently selected entry.
         */
        fun saveForCurrent() {
            if (!pathNavigator.isInitialized) return

            when (val cwd = pathNavigator.working) {
                is DirectoryListing.Listing ->
                    if (cwd.isNotEmpty)
                        history[pathNavigator.working.path] = cwd.entries[selectedItemIdx].path

                is DirectoryListing.RestrictedListing ->
                    if (cwd.isNotEmpty)
                        history[pathNavigator.working.path] = cwd.entries[selectedItemIdx].path

                is DirectoryListing.Error -> {}
            }
        }

        /**
         * Preselect the entry (child entry or previously selected).
         */
        fun restoreForCurrent() {
            val pathToPreselect = pathOfChildIfNavigatingUp() ?: history[pathNavigator.working.path]

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

        /**
         * When going "up the filesystem tree", child directory we came from should be preselected. Check if this is the
         * case, and if so return that child's Path.
         */
        private fun pathOfChildIfNavigatingUp(): Path? =
            pathNavigator.previousWorking?.let { prevWorkingPath ->
                if (pathNavigator.working.path.isParentOf(prevWorkingPath))
                    prevWorkingPath
                else
                    null
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

                else -> unreachable()
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
                else -> unreachable()
            }
            this.model = selection
            this.factory = factory
        }

        fun ListView.focusAndSelectTo(idx: Int) {
            scrollTo(idx, setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT), null)
        }

        private fun Path.isParentOf(path: Path): Boolean =
            this == path.parent
    }
}