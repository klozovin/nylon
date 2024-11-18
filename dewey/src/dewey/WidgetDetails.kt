package dewey

import dewey.fsnav.*
import org.gnome.gtk.Box
import org.gnome.gtk.Label
import org.gnome.gtk.Orientation
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.readSymbolicLink

class WidgetDetails : Box(Orientation.HORIZONTAL, 8) {
    private val prefix = Label("✕").apply { addCssClass("prefix") }
    private val permissions = Label("✕").apply { addCssClass("permissions") }
    private val owner = Label("✕").apply { addCssClass("owner") }
    private val group = Label("✕").apply { addCssClass("group") }
    private val size = Label("✕").apply { addCssClass("size") }
    private val modifiedAt = Label("✕").apply { addCssClass("modified-at") }
    private val extra = Label("")

    init {
        name = "details"
        append(prefix)
        append(permissions)
        append(owner)
        append(group)
        append(size)
        append(modifiedAt)
        append(extra)
    }

    fun clear() {
        prefix.label = "—"
        permissions.label = "—"
        owner.label = "—"
        group.label = "—"
        size.label = "—"
        modifiedAt.label = "—"
        extra.label = ""
    }

    fun update(entry: BaseDirectoryEntry.DirectoryEntry) {
        // Permissions string [rwx]
        prefix.label = when (entry) {
            is Directory -> "/"
            is File -> "-" // TODO: add "*" if executables
            is Symlink -> "@"
            is Other -> "-"
            // TODO: The rest, | -> pipe, = -> socket,
        }
        permissions.label = PosixFilePermissions.toString(entry.permissions)
        owner.label = entry.owner.name
        group.label = entry.group.name

        // Size of entry
        val sizeSuffix = if (entry is File) "B" else ""
        size.label = "${entry.attributes.size()}$sizeSuffix"

        // Last modification time
        modifiedAt.label = entry.attributes.lastModifiedTime().toString()

        // If symlink, it's target
        extra.label = if (entry is Symlink) "⇥ ${entry.path.readSymbolicLink()}" else ""
    }
}