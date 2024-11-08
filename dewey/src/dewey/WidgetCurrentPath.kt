package dewey

import org.gnome.gtk.Box
import org.gnome.gtk.Label
import org.gnome.gtk.Orientation
import java.nio.file.Path
import kotlin.io.path.pathString

class WidgetCurrentPath : Box(Orientation.HORIZONTAL, 4) {
    val current = Label("✕").apply { addCssClass("cwd") }
    val separator = Label(" → ").apply { addCssClass("separator") }
    val focused = Label("✕").apply { addCssClass("focused") }

    init {
        append(current)
        append(separator)
        append(focused)
    }

    fun updateTargetPath(path: Path) {
        // Don't show double "//" when showing the root filesystem.
        current.label = "${path.pathString}${if (path.parent != null) "/" else ""}"
    }

    fun updateFocused(path: Path) {
        focused.label = path.fileName?.pathString ?: "—"
    }

    fun clearFocused() {
        focused.label = "—"
    }
}