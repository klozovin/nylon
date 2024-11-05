package dewey

import org.gnome.gdk.Gdk
import org.gnome.gtk.Entry
import org.gnome.gtk.EventControllerKey
import org.gnome.gtk.Window
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.Path

class ChangeDirectoryDialog : Window() {

    private val directoryPathInput = Entry().apply {

        placeholderText = "/path/to/cd/into"

        // Close everything on Escape
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
        directoryPathInput.onActivate {
            val entryPath = Path(directoryPathInput.text)
            if (Files.exists(entryPath, LinkOption.NOFOLLOW_LINKS)) {
                // TODO: BUG: Race condition, should use exceptions for this
                close()
                callback(entryPath)
            } else
                println("Error: Tried to cd into non existent directory.")
        }
    }

    init {
//        directoryPathInput.onActivate {
//            println("activated")
//            close()
//        }
        child = directoryPathInput
    }
}