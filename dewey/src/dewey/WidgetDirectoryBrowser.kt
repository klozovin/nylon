package dewey

import dewey.fsnav.*
import io.github.jwharm.javagi.gio.ListIndexModel
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gobject.GObject
import org.gnome.gtk.*
import java.nio.file.Path
import kotlin.io.path.name
import dewey.fsnav.DirectoryListingResult as DirectoryListing


// TODO: Can't be singleton when multiple tabs implemented
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
    val directoryListWidget = ListView(null, null).apply {
        addController(eventController)
        onActivate { openSelectedEntry() }
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
        openDirectory(path)
    }

    // -------------------------------------------------------------------------------------------------------------- //

    private fun updateDirectoryList() {
        when (val cwd = pathNavigator.working) {
            is DirectoryListing.Listing -> {
                if (cwd.isNotEmpty)
                    directoryListWidget.showDirectoryListing(cwd.count, itemFactory)
                else
                    directoryListWidget.showErrorListing("empty directory")
            }

            is DirectoryListing.RestrictedListing -> {
                if (cwd.isNotEmpty)
                    directoryListWidget.showDirectoryListing(cwd.count, itemFactoryRestricted)
                else
                    directoryListWidget.showErrorListing("empty restricted directory")
            }

            is DirectoryListing.Error -> {
                when (cwd.err) {
                    DirectoryListing.Error.Type.AccessDenied ->
                        directoryListWidget.showErrorListing("access denied")

                    DirectoryListing.Error.Type.PathNonExistent ->
                        directoryListWidget.showErrorListing("directory does not exist")

                    // TODO: Is this the best way to handle it? - We should never have to be in this (.PND) code path!
                    DirectoryListing.Error.Type.PathNotDirectory ->
                        directoryListWidget.showErrorListing("given path is not a directory")

                    DirectoryListing.Error.Type.ChangedWhileReading -> TODO()
                }
            }
        }
    }

    private fun ListView.selectedItemIdx(): Int? =
        when (val selectionModel = this.model) {
            is SingleSelection -> {
                check(selectionModel.nItems != 0)
                selectionModel.selected
            }

            is NoSelection -> null
            else -> unreachable()
        }


    private fun ListView.selectedItem(): BaseDirectoryEntry? =
        selectedItemIdx()?.let { idx ->
            when (val cwd = pathNavigator.working) {
                is DirectoryListing.Listing -> cwd.entries[idx]
                is DirectoryListing.RestrictedListing -> cwd.entries[idx]
                is DirectoryListing.Error -> null
            }
        }


    private fun ListView.showDirectoryListing(count: Int, factory: BaseItemFactory) {
        val model = ListIndexModel.newInstance(count)
        val selection = SingleSelection<ListIndexModel.ListIndex>(model).apply {
            canUnselect = false
            onSelectionChanged(::selectionChangedHandler)
        }

        // MUST first set to null ("race" condition)
        this.model = null
        this.factory = null

        this.model = selection
        this.factory = factory
    }


    private fun ListView.showErrorListing(message: String) {
        val model = StringList(arrayOf(message))
        val selection = NoSelection<GObject>(model)

        // MUST first set to null ("race" condition)
        this.model = null
        this.factory = null

        this.model = selection
        this.factory = this@WidgetDirectoryBrowser.itemFactoryEmpty
    }

    private fun ListView.selectionUp() {
        if (model is NoSelection) return
        val idx = selectedItemIdx()!!
        if (idx > 0)
            scrollToFocusAndSelect(idx - 1)
    }

    private fun ListView.selectionDown() {
        when (val selectionModel = model) {
            is SingleSelection -> {
                val idx = selectedItemIdx()!!
                if (idx < selectionModel.nItems - 1)
                    scrollToFocusAndSelect(idx + 1)
            }

            is NoSelection -> return
            else -> unreachable()
        }
    }

    fun ListView.scrollToFocusAndSelect(idx: Int) {
        scrollTo(idx, setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT), null)
    }

    private fun showChangeDirectoryDialog() {
        ChangeDirectoryDialog().apply {
            modal = true
            onInputReceive { path ->
                openDirectory(path)
            }
            transientFor = this@WidgetDirectoryBrowser.directoryListWidget.root as Window
            present()
        }
    }

    // -------------------------------------------------------------------------------------------------------------- //

    /**
     * Perform default action (open) on selected item.
     */
    private fun openSelectedEntry() {
        when (pathNavigator.working) {
            is DirectoryListing.Listing -> {
                when (val selected = directoryListWidget.selectedItem()) {
                    is Directory -> openDirectory(selected.path)
                    is File -> openFile(selected)
                    is Other -> TODO()
                    is Symlink -> {
                        if (selected.linksToType() == "d") // TODO: Horrible
                            openDirectory(selected.path)
                    }

                    is BaseDirectoryEntry.RestrictedEntry -> unreachable()
                    null -> unreachable()
                }
            }

            is DirectoryListing.Error -> return
            is DirectoryListing.RestrictedListing -> return
        }
    }

    private fun openDirectory(dir: Path) {
        selectionHistory.saveForCurrent()

        pathNavigator.navigateTo(dir)
        updateDirectoryList()

        selectionHistory.restoreForCurrent()

        DirectoryBrowserState.cwdListing = pathNavigator.working
        DirectoryBrowserState.selectedItem = directoryListWidget.selectedItem()
    }

    private fun openParentDirectory() {
        pathNavigator.working.path.parent?.let {
            openDirectory(it)
        }
    }

    private fun reloadDirectory() {
        openDirectory(pathNavigator.working.path)
    }

    private fun openFile(entry: File) {
        println("NOT IMPLEMENTED: Opening [${entry.path.name}]")
    }

    // ------------------------------------------------------------------------ //

    @Suppress("UNUSED_PARAMETER")
    private fun keyPressHandler(keyVal: Int, keyCode: Int, modifierTypes: MutableSet<ModifierType>?): Boolean {
        when (keyVal) {
            Gdk.KEY_i -> directoryListWidget.selectionUp()
            Gdk.KEY_k -> directoryListWidget.selectionDown()

            Gdk.KEY_F5 -> reloadDirectory()
            Gdk.KEY_F6 -> showChangeDirectoryDialog()

            Gdk.KEY_Left, Gdk.KEY_j -> openParentDirectory()
            Gdk.KEY_Right, Gdk.KEY_l -> openSelectedEntry()
        }
        return false
    }

    @Suppress("UNUSED_PARAMETER")
    private fun selectionChangedHandler(position: Int, items: Int) {
        DirectoryBrowserState.selectedItem = directoryListWidget.selectedItem()
    }

    // ----- HELPER INNER CLASSES ------------------------------------------------------------------------- //


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
                        history[pathNavigator.working.path] = cwd.entries[directoryListWidget.selectedItemIdx()!!].path

                is DirectoryListing.RestrictedListing ->
                    if (cwd.isNotEmpty)
                        history[pathNavigator.working.path] = cwd.entries[directoryListWidget.selectedItemIdx()!!].path

                is DirectoryListing.Error -> {}
            }
        }

        /**
         * Preselect the entry (child entry or previously selected).
         */
        fun restoreForCurrent() {
            val pathToPreselect = pathOfChildIfNavigatingUp() ?: history[pathNavigator.working.path]

            if (pathToPreselect == null) {
                directoryListWidget.scrollToFocusAndSelect(0)
                return
            }

            when (val cwd = pathNavigator.working) {
                is DirectoryListing.Listing -> {
                    val idxSelected = cwd.entries.indexOfFirst { it.path == pathToPreselect }
                    if (idxSelected != -1)
                        directoryListWidget.scrollToFocusAndSelect(idxSelected)
                    else {
                        println("Previously selected entry [${pathToPreselect}] no longer exists in directory [${pathNavigator.working.path}]")
                        directoryListWidget.scrollToFocusAndSelect(0)
                        history.remove(pathNavigator.working.path)
                    }
                }

                is DirectoryListing.RestrictedListing -> {
                    val idxSelected = cwd.entries.indexOfFirst { it.path == pathToPreselect }
                    if (idxSelected != -1)
                        directoryListWidget.scrollToFocusAndSelect(idxSelected)
                    else {
                        println("Previously selected entry [${pathToPreselect}] no longer exists in directory [${pathNavigator.working.path}]")
                        directoryListWidget.scrollToFocusAndSelect(0)
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
     * Showing a regular directory with <rwx> permissions. Can get full details about entries.
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
        private fun Path.isParentOf(path: Path): Boolean =
            this == path.parent
    }
}