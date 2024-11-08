package dewey

import dewey.FilesystemNavigator.BaseTarget.TargetFull
import dewey.FilesystemNavigator.BaseTarget.TargetPathsOnly
import dewey.FilesystemNavigator.EntryType
import io.github.jwharm.javagi.gio.ListIndexModel
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gobject.GObject
import org.gnome.gtk.*
import java.nio.file.Path
import kotlin.io.path.name


class WidgetDirectoryBrowser(path: Path) {

    val pathNavigator = FilesystemNavigator()

    /**
     * For a given directory, remember the last selected item when the user navigated away from it. Used for easier
     * movement through the file system.
     */
    private val directorySelectionHistory: MutableMap<Path, Path> = mutableMapOf()

    private val eventController = EventControllerKey().apply {
        onKeyPressed(::keyPressHandler)
    }

    //
    // Top: Show current directory and selected item
    //
    val currentPathAndSelectionWidget = WidgetCurrentPath()

    //
    // Middle: List of directories/files in the current directory
    //
    val directoryListWidget = ListView(null, null).apply {
        addController(eventController)
    }

    // Helper class for managing ListView state
    val listViewState: ListViewState = ListViewState()


    // Scroll container for the listing
    val scrolledWidget = ScrolledWindow().apply {
        child = directoryListWidget
        vexpand = true
    }

    //
    // Bottom: Details about the selected item (directory/file)
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
        // TODO: maybe better to use navigator here to get to path, then have a pure UI function that reads target
        //       and populates the list view
        // TODO: Set the factory depending on the kind of dir user opened at startup
//        directoryListWidget.factory = itemFactoryRegular
        openDirectory(FilesystemNavigator.EntryFull.of(path))
    }

    private fun openEntry(entry: FilesystemNavigator.EntryFull) {
        when (entry.type) {
            EntryType.Regular -> openFile(entry)
            EntryType.Directory -> openDirectory(entry)
            EntryType.Symlink -> {
                when (entry.linksToType()) {
                    EntryType.Directory -> openDirectory(entry)
                    else -> println("Don't know what to do with link: [${entry.path}]")
                }
            }

            else -> println("Don't know what to do with [${entry.path.name}]")
        }
    }

    private fun openDirectory(entry: FilesystemNavigator.EntryFull) {
        println("Opening [${entry.path.name}]")

        // TODO: Selection history for easier navigation

        pathNavigator.navigateTo(entry.path) // TODO: do this somewhere else, this function should just update UI

        when (val target = pathNavigator.target) {
            is TargetFull -> {
                listViewState.update(target)
                scrollTo(0)
                currentPathAndSelectionWidget.updateTargetPath(target.path)
                if (target.isNotEmpty) {
                    currentPathAndSelectionWidget.updateFocused(listViewState.selectedItem) // TODO: just use item || entry
                    selectedItemDetails.update(listViewState.selectedEntry)
                } else {
                    currentPathAndSelectionWidget.clearFocused() // TODO: use clearFocused()
                    selectedItemDetails.clear()
                }
            }

            is TargetPathsOnly -> {
                listViewState.update(target)
                scrollTo(0) // TODO: Scroll to remembered
                currentPathAndSelectionWidget.updateTargetPath(target.path)
                if (target.isNotEmpty) {
                    val selectedItem = target.entries[listViewState.selectionModelLimited.selected]
                    currentPathAndSelectionWidget.updateFocused(selectedItem.path)
                    selectedItemDetails.clear()
                } else {
                    currentPathAndSelectionWidget.clearFocused()
                    selectedItemDetails.clear()
                }
                // disable actdivation, events, selection, only back should work
            }

            is FilesystemNavigator.BaseTarget.TargetNotAccessible -> {
                println("target not accessible")
            }

            is FilesystemNavigator.BaseTarget.TargetInvalid -> TODO()
        }


        // TODO: Delete this, keep only this function
//        navigateTo(entry.path)
    }

    /**
     * Go up in directory hierarchy.
     */
    private fun openParentDirectory() {
        // TODO: This lists directory contents twice (openDirectory() does it again)
        pathNavigator.navigateToParent()
        openDirectory(pathNavigator.target.entry)
    }

    private fun openFile(entry: FilesystemNavigator.EntryFull) {
        println("Opening [${entry.path.name}]")
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

    // ------------------------------------------------------------------------ //

    /**
     * Go to new directory and show its contents.
     */
    private fun navigateTo(target: Path) {
        println("Navigating to directory: ${target}")

        /*

        // Still have the old state and Path, save it now
        if (::listViewState.isInitialized && !pathNavigator.targetFull.isEmpty)
            directorySelectionHistory[pathNavigator.target.path] = listViewState.selectedItem
//            directorySelectionHistory.addAll(arrayOf(state.selectedItem, state.path))
         */


        // TODO: State could mark this on startup so that we don't iterate twice over the directory list
        //       -- move it to new FilesystemNavigator class
        pathNavigator.navigateTo(target) // TODO: handle errors here
        // TODO: Continue here, we should not be able to CD into this


//        listViewState = ListViewState()
        directoryListWidget.model = listViewState.selectionModelRegular

        // Restore previous selection in this directory.
        directorySelectionHistory.get(pathNavigator.target.path)?.let { savedSelection ->
            // There was previously saved selection, but that doesn't mean this Path still exists. It could've been
            // deleted or renamed while we were showing another directory. If the previously saved selection doesn't
            // exist in the current directory listing, delete it.
            val idxSelectedPath = pathNavigator.targetFull.indexOf(savedSelection)
            if (idxSelectedPath != -1) {
                // Found it, move the ListView selection *and* focus to it.
                scrollTo(idxSelectedPath)
            } else {
                // Item was saved, but it's no longer in the directory. Clear it.
                directorySelectionHistory.remove(savedSelection)
            }
        }

        // Update top and bottom widgets
        // TODO: move this to some common function set selected
        currentPathAndSelectionWidget.updateTargetPath(pathNavigator.target.path)

        // Current directory is NOT empty
        if (!pathNavigator.targetFull.isEmpty) {
            currentPathAndSelectionWidget.updateFocused(listViewState.selectedItem)

            // TODO back here
            if (listViewState.selectedEntry.type != null)
                selectedItemDetails.update(listViewState.selectedEntry)
            else
                selectedItemDetails.clear()
        } else {
            // Current directory empty: clear info at bottom, and selected in path on top
            currentPathAndSelectionWidget.clearFocused()
            selectedItemDetails.clear()
        }
    }


    private fun scrollTo(idx: Int) =
        directoryListWidget.scrollTo(idx, setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT), null)

    /**
     * Reload current directory.
     */
    private fun reloadDirectory() {
        println("Reloading: ${pathNavigator.target.path}")
        navigateTo(pathNavigator.target.path)
    }

    //

    private fun keyPressHandler(keyVal: Int, keyCode: Int, modifierTypes: MutableSet<ModifierType>?): Boolean {
        when (keyVal) {
            // TODO: extract to function
            Gdk.KEY_i -> if (listViewState.selectedItemIdx > 0) scrollTo(listViewState.selectedItemIdx - 1)
            Gdk.KEY_k -> if (listViewState.selectedItemIdx < listViewState.selectionModelRegular.nItems - 1) scrollTo(
                listViewState.selectedItemIdx + 1
            )

            Gdk.KEY_F5 -> reloadDirectory()
            Gdk.KEY_F6 -> showChangeDirectoryDialog()

            Gdk.KEY_Left, Gdk.KEY_j -> openParentDirectory()
            Gdk.KEY_Right, Gdk.KEY_l -> openSelectedListViewItem()
        }
        return false
    }

    private fun selectionChangedHandler(position: Int, items: Int) {
        println("Selection change handler called")
        // Check if inside empty-directory: ListView is not empty, but that item doesn't represent a real dir/file.
        if (pathNavigator.targetFull.isEmpty) {
            currentPathAndSelectionWidget.clearFocused()
            selectedItemDetails.clear()
            return
        }
        // Update: current path / item name, bottom info
        currentPathAndSelectionWidget.updateFocused(listViewState.selectedItem)
        selectedItemDetails.update(listViewState.selectedEntry)
    }

    /**
     * Handle activating an item in the browser.
     */

    // TODO Join these two functions, should be one place where item gets activated
    private fun openSelectedListViewItem() {
        when (val target = pathNavigator.target) {
            is TargetFull -> if (target.isNotEmpty) openEntry(target.entries[listViewState.selectionModelRegular.selected])
            is TargetPathsOnly -> return
            is FilesystemNavigator.BaseTarget.TargetNotAccessible -> TODO()
            is FilesystemNavigator.BaseTarget.TargetInvalid -> TODO()
        }
    }

    private fun activateHandler(activatedIdx: Int) {
        println("activatedIdx: ${activatedIdx}")
        // Empty directory, nothing to activate, there's a dummy element in ListView (hacky?)
        // TODO: Can't activate entry in a [rw-] directory
        if (pathNavigator.targetFull.isEmpty)
            error("Unreachable")


        // TODO: Handle invalid list items here? (empty dir, no-execute dir, etc?)
        val activatedEntry = pathNavigator.targetFull.entries[activatedIdx]
        openEntry(activatedEntry)
    }

    //
    // HELPER CLASSES
    //

    inner class ListViewState {
        //
        // Models, selection, factories used for: regular listing, restricted listing, empty/inaccessible/etc
        //
        val itemFactoryEmpty = ItemFactoryEmptyDirectory()
        val listModelEmpty = StringList<StringObject>(arrayOf("empty"))
        val selectionModelEmpty = NoSelection<StringObject>(listModelEmpty)

        // Limited
        val itemFactoryLimited = ItemFactoryRestrictedDirectory()
        var listModelLimited = ListIndexModel.newInstance(0)
        val selectionModelLimited =
            SingleSelection<ListIndexModel.ListIndex>(listModelLimited).apply { canUnselect = false }

        // Regular ListView
        val itemFactoryRegular = ItemFactoryRegularDirectory()
        val onActivateSignalConnection = directoryListWidget.onActivate(::activateHandler)
        var listModelRegular = ListIndexModel.newInstance(0)

        //        val selectionChangedRegularSignalConnection: SignalConnection<SelectionChangedCallback>
        var selectionModelRegular = SingleSelection<ListIndexModel.ListIndex>(listModelRegular).apply {
            canUnselect = false
//            selectionChangedRegularSignalConnection =
            onSelectionChanged(::selectionChangedHandler)
//            println(selectionChangedRegularSignalConnection)
        } // TODO: Can newer version of java-gi instantiate with null for model?


        val selectedItemIdx: Int
            get() = selectionModelRegular.selected

        val selectedEntry: FilesystemNavigator.EntryFull
            get() = (pathNavigator.target as TargetFull).entries[selectionModelRegular.selected]

        val selectedItem: Path
            get() = pathNavigator.targetFull.entries[selectionModelRegular.selected].path

        /**
         * Called when showing a regular directory with full permissions
         */
        fun update(target: TargetFull) {
            if (target.isNotEmpty) {
                listModelRegular = ListIndexModel.newInstance(target.count)
                selectionModelRegular.model = listModelRegular
                directoryListWidget.setModelAndFactory(selectionModelRegular, itemFactoryRegular)
                onActivateSignalConnection.unblock()
            } else {
                onActivateSignalConnection.block()
                directoryListWidget.setModelAndFactory(selectionModelEmpty, itemFactoryEmpty)
            }
        }

        /**
         * Called when showing a [rw-] directory, we can only list paths and nothing else
         */
        fun update(target: TargetPathsOnly) {
            println("TARGET: ${target.isNotEmpty}")
            println("IVEGOT THIS: ${target.entries}")
            if (target.isNotEmpty) {
                listModelLimited = ListIndexModel.newInstance(target.count)
                selectionModelLimited.model = listModelLimited
                directoryListWidget.setModelAndFactory(selectionModelLimited, itemFactoryLimited)
                onActivateSignalConnection.block()
            } else {
                onActivateSignalConnection.block()
                directoryListWidget.setModelAndFactory(selectionModelEmpty, itemFactoryEmpty)
            }

        }
    }

    //
    // Item Factories
    //

    abstract class BaseItemFactory: SignalListItemFactory() {
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
            when (val target = pathNavigator.target) {
                is TargetPathsOnly -> {
                    val itemEntry = target.entries[listItem.position]
                    listItemLabel.apply {
                        label = "% ${itemEntry.name}"
                        addCssClass("nonxdirentry")
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

            // Showing an empty directory
            if (pathNavigator.targetFull.isEmpty) {
                check(listViewState.listModelRegular.nItems == 1) { "On empty directory ListView should have only one element inside." }
                listItemLabel.label = "<< empty >>"
                listItemLabel.addCssClass("empty")
                return
            }

            check(item.index == listItem.position)

            // TODO: Maybe us this instead of a property?
//            (pathNavigator.target as FilesystemNavigator.TargetFull).let { target ->
//                println(it.entries)
//                println(it.entries.first())
//            }

            val itemEntry = pathNavigator.targetFull.entries[listItem.position]
            when (itemEntry.type) {
                EntryType.Symlink -> {
                    listItemLabel.label = when (itemEntry.linksToType()) {
                        EntryType.Directory -> "[[⇥ ${itemEntry.name}]]"
                        else -> "⇥ ${itemEntry.name}"
                    }
                    listItemLabel.addCssClass("symlink")
                }

                EntryType.Directory -> listItemLabel.apply {
                    label = "[[${itemEntry.name}]]"
                    addCssClass("directory")
                }

                EntryType.Regular -> listItemLabel.apply {
                    label = itemEntry.name
                    addCssClass("file")
                }

                EntryType.Other -> listItemLabel.apply {
                    label = itemEntry.name
                    addCssClass("unknown")
                }

                else -> TODO()
            }
            check(listItemLabel.cssClasses.size <= 1) { "Can't have more than one class set" }
        }
    }

    companion object {
        /**
         * Have to do it by setting to null first, otherwise there's a race condition.
         */
        fun <T : GObject> ListView.setModelAndFactory(selectionModel: SelectionModel<T>, itemFactory: ListItemFactory) {
            this.model = null
            this.factory = null
            this.model = selectionModel
            this.factory = itemFactory
        }
    }
}