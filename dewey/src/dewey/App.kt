package dewey

import org.gnome.gdk.Display
import org.gnome.gio.ApplicationFlags
import org.gnome.gtk.*
import kotlin.io.path.*


val styleCss = """
    window {
        font-size: 18px;
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
    
    box.cwd {
        color: #00ff00;
        font-weight: bold;
    }
    box.focused {
        color: #000000;
        font-style: italic;
    }
"""


fun main(args: Array<String>) {
    println("Running with arguments: ${args.contentToString()}")
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

        // Use home directory if nothing passed on the command line
        val homeDirectory = Path(System.getProperty("user.home"))
        val startupDirectory = args.getOrNull(0)?.let { Path(it) } ?: homeDirectory

        val browser = WidgetDirectoryBrowser(startupDirectory)
        window.child = browser.boxWidget
        window.present()
    }

    app.onShutdown {
        println("App shutting down")
    }

    app.run(args)
}