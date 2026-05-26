package compositor.inspector

import org.gnome.gdk.DisplayManager
import org.gnome.gtk.Application
import org.gnome.gtk.ApplicationWindow
import org.gnome.gtk.Gtk
import org.gnome.gtk.Label
import kotlin.concurrent.thread


class Inspector(val socket: String) {

    fun run() {
        thread {
            // Hack: wait for the compositor to initialize
            Thread.sleep(1000)

            // Hack to tell the GTK where to find the Wayland display server since we're running the GTK
            // app from within the compositor process itself.
            Gtk.init()
            DisplayManager.get().apply {
                defaultDisplay = openDisplay(socket) ?: error("GTK failed to open display")
            }

            val app = Application("com.sklogw.nylon.compositor.Inspector")
            app.onActivate {
                ApplicationWindow(app).apply {
                    title = "Inspector"
                    setDefaultSize(600, 400)
                    child = Label("Compositor Inspector -- Running within the compositor process")
                    present()
                }
            }
            app.run(emptyArray<String>())
        }
    }
}