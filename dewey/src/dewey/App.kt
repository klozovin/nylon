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
    }
    .file {
        color: #0000ff;
    }
    .empty {
        color: #000000;
        font-style: italic;
    }
"""

class DirectoryBrowser(path: Path) {

    private var state = State(path)
    private val itemFactory = ItemFactory()

    private val eventController = EventControllerKey().apply {
        onKeyPressed(::keyPressHandler)
    }

    val widget = ListView(state.selectionModel, itemFactory).apply {
        onActivate(::activateHandler)
        addController(eventController)
    }

    /**
     * Handle keyboard shortcuts in browser.
     */
    private fun keyPressHandler(keyVal: Int, keyCode: Int, modifierTypes: MutableSet<ModifierType>?): Boolean {
        when (keyVal) {
            Gdk.KEY_Left -> navigateToParent()
            Gdk.KEY_Right -> widget.emitActivate(state.selectionModel.selected)
        }
        println("> Key: [$keyVal]: ${Gdk.keyvalName(keyVal)}, $keyCode, $modifierTypes")
        return false
    }

    /**
     * Handle activating an item in the browser.
     */
    private fun activateHandler(idx: Int) {
        val activatedPath = state.dirList[idx]

        // Skip files when activated
        if (!activatedPath.isDirectory())
            return

        println("> Activated: [${activatedPath}]")
        navigateTo(activatedPath)
    }

    /**
     * Go to new directory and show its contents.
     */
    private fun navigateTo(target: Path) {
        println("Navigating to directory: ${target}")
        state = State(target)
        widget.model = state.selectionModel
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
    class State(val path: Path) {
        val dirList = path.listDirectoryEntries().sortedByDescending { it.isDirectory() }
        val isEmpty = dirList.isEmpty()

        // Handle empty directories by adding a dummy item to ListView model
        val dirListModel =
            if (!isEmpty) StringList(dirList.map { pathToString(it) }.toTypedArray())
            else StringList(arrayOf("<< empty folder >>"))

        val selectionModel = SingleSelection(dirListModel)

        private fun pathToString(path: Path): String =
            if (path.isDirectory())
                "[[${path.toString()}]]"
            else
                path.toString()
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
            println("Creating a new label")
        }

        private fun bind(listItem: ListItem) {
            val label = listItem.child as Label
            val item = listItem.item as StringObject

            label.label = item.string
            check(label.cssClasses.size <= 1) { "Can't have more than one class set" }

            if (!state.isEmpty) {
                if (state.dirList[listItem.position].isDirectory())
                    label.addCssClass("directory")
                else
                    label.addCssClass("file")
            } else {
                check(state.dirListModel.nItems == 1) { "On empty directory ListView should have only one element inside." }
                label.addCssClass("empty")
            }
            println("Postion: ${listItem.position}")
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
        val scrolled = ScrolledWindow()

        val browser = DirectoryBrowser(homeDirectory)

        scrolled.child = browser.widget
        window.child = scrolled
        window.present()
    }

    app.onShutdown {
        println("App shutting down")
    }

    app.run(args)
}

/*

class MyTreeView

class ListItem<I, C>() where
    I: ModelProvides,
    C: org.gnome.Widget
{

ListItem

 */

//val listItemFactoryOuter = SignalListItemFactory().apply {
//    onSetup {
//        val listItem = it as ListItem
//        listItem.child = Label("")
//        println("item setup")
//    }
//
//    onBind {
//        val listItem = it as ListItem
//        val label = listItem.child as Label?
//        val item = listItem.item as StringObject?
//
//        if (label == null || item == null) {
//            println("!!! Should never happen??? !!!")
//            return@onBind
//        }
//
//        label.label = item.string
//    }
//}
