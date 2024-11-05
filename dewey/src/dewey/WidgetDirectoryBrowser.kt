package dewey

import io.github.jwharm.javagi.gio.ListIndexModel
import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gtk.*
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
import kotlin.io.path.name
import dewey.FilesystemNavigator.Entry.Type as EntryType

class WidgetDirectoryBrowser(path: Path) {

    val pathNavigator = FilesystemNavigator()
    private lateinit var state: CurrentDirectoryState

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
    val directoryListWidget = ListView(null, ItemFactory()).apply {
        onActivate(::activateHandler)
        addController(eventController)
    }

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
        navigateTo(path)
    }

    /**
     * Handle keyboard shortcuts in browser.
     */
    private fun keyPressHandler(keyVal: Int, keyCode: Int, modifierTypes: MutableSet<ModifierType>?): Boolean {
        when (keyVal) {
            Gdk.KEY_i -> if (state.selectedItemIdx > 0) scrollTo(state.selectedItemIdx - 1)
            Gdk.KEY_k -> if (state.selectedItemIdx < state.selectionModel.nItems - 1) scrollTo(state.selectedItemIdx + 1)

            Gdk.KEY_F5 -> reloadDirectory()
            Gdk.KEY_F6 -> showChangeDirectoryDialog()

            Gdk.KEY_Left, Gdk.KEY_j -> navigateToParent()
            Gdk.KEY_Right, Gdk.KEY_l -> directoryListWidget.emitActivate(state.selectionModel.selected)
        }
        return false
    }

    private fun openSelectedEntry() {

    }

    /**
     * Opens an entry:
     * - Directory : change to it, list its contents
     * - Executable : try to run it?
     * - File : try to open it using a default program?
     */
    private fun openEntry(entry: FilesystemNavigator.Entry) {
        when (entry.type) {
            EntryType.Regular -> openFile(entry)
            EntryType.Directory -> openDirectory(entry)
            else -> println("Don't know what to do with [${entry.path.name}]")
        }
    }

    private fun openDirectory(entry: FilesystemNavigator.Entry) {
        println("Opening [${entry.path.name}]")
        navigateTo(entry.path)
    }

    private fun openFile(entry: FilesystemNavigator.Entry) {
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

        // Still have the old state and Path, save it now
        if (::state.isInitialized && !pathNavigator.target.isEmpty)
            directorySelectionHistory[pathNavigator.target.path] = state.selectedItem
//            directorySelectionHistory.addAll(arrayOf(state.selectedItem, state.path))


        // TODO: State could mark this on startup so that we don't iterate twice over the directory list
        //       -- move it to new FilesystemNavigator class
        pathNavigator.navigateTo(target) // TODO: handle errors here
        state = CurrentDirectoryState()
        directoryListWidget.model = state.selectionModel

        // Restore previous selection in this directory.
        directorySelectionHistory.get(pathNavigator.target.path)?.let { savedSelection ->
            // There was previously saved selection, but that doesn't mean this Path still exists. It could've been
            // deleted or renamed while we were showing another directory. If the previously saved selection doesn't
            // exist in the current directory listing, delete it.
            val idxSelectedPath = pathNavigator.target.indexOf(savedSelection)
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
        currentPathAndSelectionWidget.updateCurrent(pathNavigator.target.path)

        // Current directory is NOT empty
        if (!pathNavigator.target.isEmpty) {
            currentPathAndSelectionWidget.updateFocused(state.selectedItem)

            // TODO back here
            if (state.selectedEntry.type != null)
                selectedItemDetails.update(state.selectedEntry)
            else
                selectedItemDetails.clear()
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
        val parent = pathNavigator.target.path.parent ?: return // Do nothing when currently in root (/)
        navigateTo(parent)
    }

    /**
     * Reload current directory.
     */
    private fun reloadDirectory() {
        println("Reloading: ${pathNavigator.target.path}")
        navigateTo(pathNavigator.target.path)
    }

    /**
     * Handle activating an item in the browser.
     */
    private fun activateHandler(activatedIdx: Int) {
        // Empty directory, nothing to activate, there's a dummy element in ListView (hacky?)
        // TODO: Can't activate entry in a [rw-] directory
        if (pathNavigator.target.isEmpty)
            return
        // TODO: Handle invalid list items here? (empty dir, no-execute dir, etc?)
        val activatedEntry = pathNavigator.target.entries!![activatedIdx]
        openEntry(activatedEntry)
    }

    /**
     * Updates the UI when the selection changes.
     *
     * Called by:
     * - SelectionModel selection-changed signal.
     * - DirectoryBrowser, when navigating to a directory
     */
    private fun selectionChangedHandler() {
        println("Selection change handler called")
        // Check if inside empty-directory: ListView is not empty, but that item doesn't represent a real dir/file.
        if (pathNavigator.target.isEmpty) {
            currentPathAndSelectionWidget.updateFocused(null)
            selectedItemDetails.clear()
            return
        }

        // Update: current path / item name, bottom info
        currentPathAndSelectionWidget.updateFocused(state.selectedItem)

        if (state.selectedEntry.type != null)
            selectedItemDetails.update(state.selectedEntry)
    }

    private fun scrollTo(idx: Int) =
        directoryListWidget.scrollTo(idx, setOf(ListScrollFlags.FOCUS, ListScrollFlags.SELECT), null)

    /**
     * Keep the state related to single directory view in one place. Don't mutate, recreate.
     */
    inner class CurrentDirectoryState {
        val dirListModel =
            ListIndexModel.newInstance(if (!pathNavigator.target.isEmpty) pathNavigator.target.count else 1)

        val selectionModel = SingleSelection(dirListModel).apply {
            onSelectionChanged { _, _ -> selectionChangedHandler() }
        }

        val selectedItemIdx: Int
            get() = selectionModel.selected

        val selectedEntry: FilesystemNavigator.Entry
            get() = pathNavigator.target.entries!![selectionModel.selected]

        val selectedItem: Path
            get() = pathNavigator.target.entries!![selectionModel.selected].path

        val selectedItemAttributes: PosixFileAttributes
            get() = pathNavigator.target.entries!![selectionModel.selected].attributes!!

        init {
            println("Creating [CurrentDirectoryState]")
        }
    }

    /**
     * Creates and updates rows in virtualized ListView control.
     */
    inner class ItemFactory : SignalListItemFactory() {
        init {
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
            if (pathNavigator.target.isEmpty) {
                check(state.dirListModel.nItems == 1) { "On empty directory ListView should have only one element inside." }
                listItemLabel.label = "<< empty >>"
                listItemLabel.addCssClass("empty")
                return
            }

            check(item.index == listItem.position)

            val itemEntry = pathNavigator.target.entries!![listItem.position]
            val entryName = itemEntry.path.name

            when (itemEntry.type) {
                EntryType.Symlink -> {
                    if (itemEntry.isDirectory)
                        listItemLabel.label = "[[⇥ $entryName]]"
                    else
                        listItemLabel.label = "⇥ $entryName"
                    listItemLabel.addCssClass("symlink")
                }

                EntryType.Directory -> listItemLabel.apply {
                    label = "[[$entryName]]"
                    addCssClass("directory")
                }

                EntryType.Regular -> listItemLabel.apply {
                    label = entryName
                    addCssClass("file")
                }

                null -> listItemLabel.apply {
                    label = entryName
                    addCssClass("unknown")
                }

                else -> TODO()
            }

//            when {
//                itemEntry.isSymbolicLink -> {
//                    if (itemEntry.isDirectory)
//                        listItemLabel.label = "[[⇥$entryName]]"
//                    else
//                        listItemLabel.label = "⇥ $entryName"
//                    listItemLabel.addCssClass("symlink")
//                }
//
//                itemEntry.isDirectory -> {
//                    listItemLabel.addCssClass("directory")
//                    listItemLabel.label = "[[$entryName]]"
//                }
//
//                itemEntry.isRegularFile -> {
//                    listItemLabel.label = entryName
//                    listItemLabel.addCssClass("file")
//                }
//
//                else -> {
//                    listItemLabel.label = entryName
//                    listItemLabel.addCssClass("unknown")
//                }
//            }
            check(listItemLabel.cssClasses.size <= 1) { "Can't have more than one class set" }
        }
    }
}