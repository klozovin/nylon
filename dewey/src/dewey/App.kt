package dewey

import org.gnome.gdk.Gdk
import org.gnome.gdk.ModifierType
import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.*
import java.nio.file.Path

import kotlin.io.path.*


val homeDirectory = Path(System.getProperty("user.home"))

class DirectoryBrowser(var path: Path) {

    private var state = State(path)


    private var directoryList = path.listDirectoryEntries()

    private var directoryListModel = StringList(directoryList.map { pathToString(it) }.toTypedArray())
    private var selectionModel = SingleSelection(directoryListModel)
    private val itemFactory = ItemFactory()


    private val eventController = EventControllerKey()
    val widget = ListView(selectionModel, itemFactory)

    init {
        widget.onActivate(::activateHandler)
        eventController.onKeyPressed(::keyPressHandler)
        widget.addController(eventController)
    }

    private fun keyPressHandler(keyVal: Int, keyCode: Int, modifierTypes: MutableSet<ModifierType>?): Boolean {
        when (keyVal) {
            Gdk.KEY_Left -> navigateToParent()
            Gdk.KEY_Right -> widget.emitActivate(selectionModel.selected)
        }
        println("> Key: [$keyVal]: ${Gdk.keyvalName(keyVal)}, $keyCode, $modifierTypes")
        return false
    }

    private fun activateHandler(idx: Int) {
        val activatedPath = directoryList[idx]

        // Skip files
        if (!activatedPath.isDirectory())
            return

        println("> Activated: [${activatedPath}]")
        navigateTo(activatedPath)
    }

    private fun navigateTo(target: Path) {
        println("Navigating to: ${target}")
        path = target
        directoryList = path.listDirectoryEntries()
        directoryListModel = StringList(directoryList.map { pathToString(it) }.toTypedArray())
        selectionModel = SingleSelection(directoryListModel)
        widget.model = selectionModel
    }

    private fun navigateToParent() {
        val parent = path.parent ?: return
        navigateTo(parent)
    }

    private fun pathToString(path: Path): String =
        if (path.isDirectory())
            "[[${path.toString()}]]"
        else
            path.toString()

    // TODO: Use this class to keep everything in one place, don't mutate, recreate
    class State(val path: Path) {
        val dirList = path.listDirectoryEntries()
        val dirListModel = StringList(dirList.map { pathToString(it) }.toTypedArray())
        val selectionModel = SingleSelection(dirListModel)

        private fun pathToString(path: Path): String =
            if (path.isDirectory())
                "[[${path.toString()}]]"
            else
                path.toString()
    }

    class ItemFactory : SignalListItemFactory() {
        init {
            onSetup { setup(it as ListItem) }
            onBind { bind(it as ListItem) }
        }

        private fun setup(listItem: ListItem) {
            listItem.child = Label("")
        }

        private fun bind(listItem: ListItem) {
            val label = listItem.child as Label
            val item = listItem.item as StringObject
            label.label = item.string
        }
    }
}


fun main(args: Array<String>) {
    val app = Application("com.sklogw.nylon.Dewey", ApplicationFlags.DEFAULT_FLAGS)

    app.onActivate {
        val window = ApplicationWindow(app)
        val scrolled = ScrolledWindow()

        val browser = DirectoryBrowser(homeDirectory)

        scrolled.child = browser.widget
        window.child = scrolled
        window.present()
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
