package dewey

import org.gnome.gdk.Gdk
import org.gnome.glib.GLib
import org.gnome.gtk.Entry
import org.gnome.gtk.EventControllerKey
import org.gnome.gtk.Window
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

typealias DialogCallback = (Path) -> Unit

class ChangeDirectoryDialog : Window() {
    private var inputPath: Path? = null
    private lateinit var callback: DialogCallback

    private val directoryPathInput = Entry().apply {
        placeholderText = "/path/to/cd/into"

        // Abort on ESC
        addController(EventControllerKey().apply {
            onKeyPressed { keyVal, _, _ ->
                when (keyVal) {
                    Gdk.KEY_Escape -> close()
                }
                return@onKeyPressed false
            }
        })
    }

    fun onInputReceive(callback: (Path) -> Unit) {
        this.callback = callback

        directoryPathInput.onActivate {
            val entryPath = Path(directoryPathInput.text)
            if (entryPath.exists(NOFOLLOW_LINKS)) {
                inputPath = entryPath
                // HACK: Fix some kind of GTK race condition when calling ListView.setFocusTo() while
                //        this dialog is still visible?
                GLib.timeoutAddOnce(10) {
                    callback(entryPath)
                }
                close()
            } else
                println("Error: Tried to cd into non-existent directory.")
        }
    }

    init {
        child = directoryPathInput
    }
}