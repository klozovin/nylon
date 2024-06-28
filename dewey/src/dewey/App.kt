package dewey

import io.github.jwharm.javagi.gio.ListIndexModel
import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.*


val listOfItems = listOf("first", "second", "third", "fifth")

val listItemFactory = SignalListItemFactory().apply {
    onSetup {
        val listItem = it as ListItem
        listItem.child = Label("")
    }

    onBind {
        val listItem = it as ListItem
        val label = listItem.child as Label?
        val item = listItem.item as ListIndexModel.ListIndex?

        if (label == null || item == null) return@onBind

        label.label = listOfItems[item.index]
    }
}

//val stringListModel = StringList(listOfItems.toTypedArray())

fun main(args: Array<String>) {
    val app = Application("com.sklogw.nylon.Dewey", ApplicationFlags.DEFAULT_FLAGS)
    app.onActivate {
        val win = ApplicationWindow(app)

        // Construct the list view
        val model = ListIndexModel.newInstance(listOfItems.size)
        val singleSelection = SingleSelection(model)
        val listView = ListView(singleSelection, listItemFactory)

        win.child = listView
        win.present()
    }
    app.run(args)
}
