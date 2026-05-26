package dewey

import org.gnome.gdk.Display
import org.gnome.gdk.Gdk
import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.*
import kotlin.io.path.Path

// font-family: Adwaita Sans;

val styleCss = """
    window {
        font-family: Inter Regular;
        font-size: 16px;
        
    }
    .directory {
        color: #ff0000;
        font-weight: bold;
    }
    .file {
        color: #0000ff;
    }
    .symlink {
        color: #2dcee3;
        font-style: italic;
    }
    .unknown {
        color: #000000;
    }
    .empty {
        color: #000000;
        font-style: italic;
    }
    .restricted {
        color: #6b6b6b;
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

/*

async loading

texture.newfromfilename but in a task_run_in_thread (how to cancel?)


 */


fun main3(args: Array<String>) {
    val app = Application("com.sklogw.nylon.Dewey", ApplicationFlags.DEFAULT_FLAGS)

    app.onActivate {
        val window = ApplicationWindow(app)

        val filename = "/home/karlo/inbox/date.jpg"

//        val file = File.newForPath(filename)

        val picture = Picture()
        picture.contentFit = ContentFit.SCALE_DOWN
        picture.setFilename(filename)

        window.child = picture
        window.present()

    }

    app.run(args)
}


fun main(args: Array<String>) {
    println("Running with arguments: ${args.contentToString()}")
    val app = Application("com.sklogw.nylon.Dewey", ApplicationFlags.DEFAULT_FLAGS)
    val css = CssProvider().apply {
        onParsingError { section, error -> println("CSS parsing error: $section, ${error!!.readMessage()}") }
        loadFromString(styleCss)
    }

    app.onStartup {
        println("App startup")
    }

    app.onActivate {
        println("App activating")
        // TODO: What replaced this?
//        Display.getDefault().addpro

//        StyleContext.addProviderForDisplay(Display.getDefault(), css, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION)
        Gtk.styleContextAddProviderForDisplay(Display.getDefault(), css, 0)


        val window = ApplicationWindow(app)

        // Use home directory if nothing passed on the command line
//        val startupDir = args.getOrNull(0) ?: System.getProperty("user.home")
        val startupDir = args.getOrElse(0) { System.getProperty("user.home") }
        val startupDirPath = Path(startupDir)


        // Left: Browser
        val left = WidgetDirectoryBrowser(startupDirPath)


        val right = WidgetImageView()
//        val right = Picture().apply { setFilename("/home/karlo/inbox/date.jpg") }

        val paned = Paned().apply {
            startChild = left.boxWidget
            endChild = right.widget
            onNotify("max-position") {
                val maxPosition = getProperty("max-position") as Int
                position = maxPosition / 2

            }
        }


        // Keymap
        window.addController(EventControllerKey().apply {
            onKeyPressed { keyVal, keyCode, types ->
                when (keyVal) {
                    Gdk.KEY_F1 -> {
                        println("tutesmo")
                        paned.endChild = if (paned.endChild != null) null else right.widget
                    }
                }
                return@onKeyPressed false
            }
        })



        /*

        val label = Label.builder().setLabel("Label 111").build()
        label.hexpand =false

        // Right: Picture
        val picture = Picture()
        picture.setFilename("/home/karlo/inbox/date.jpg")
        val text = Text.builder().setText("Text 222").build()
        text.hexpand = true

        // Panes
        val pane = Paned(Orientation.HORIZONTAL)
        pane.startChild = label
        pane.endChild = text

        pane.onShow { println(pane.allocatedWidth) }
        label.onShow { println(label.allocatedWidth) }

        pane.connect("show") {println(pane.allocatedWidth)}
//        pane.connect("notify") {println(pane.allocatedWidth)}
        pane.onNotify("max-position") { param ->
            val max_position = pane.getProperty("max-position") as Int
            pane.position = max_position / 2
            println("width:  ${pane.width}, max-position: $max_position")
        }
        pane.hexpand = true
         */

        window.child = paned
        window.present()
    }

    app.onShutdown {
        println("App shutting down")
    }

    app.run(args)
}