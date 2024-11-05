package dewey

import org.gnome.gtk.Box
import org.gnome.gtk.Label
import org.gnome.gtk.Orientation
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
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
        prefix.label = "✕"
        permissions.label = "✕"
        owner.label = "✕"
        group.label = "✕"
        size.label = "✕"
        modifiedAt.label = "✕"
        extra.label = ""
    }

    fun update(path: Path, attributes: PosixFileAttributes) {
        // MAYBE: Clear (reset to default?) before every update?
        prefix.label = when {
            attributes.isSymbolicLink -> "l"
            attributes.isDirectory -> "d"
            attributes.isRegularFile -> "-"
            else -> "-"
        }

        permissions.label = PosixFilePermissions.toString(attributes.permissions())
        owner.label = attributes.owner().name
        group.label = attributes.group().name

        val sizeSuffix = if (!attributes.isDirectory) "B" else ""
        size.label = "${attributes.size()}$sizeSuffix"

        modifiedAt.label = attributes.lastModifiedTime().toString()

        if (attributes.isSymbolicLink)
            extra.label = "⇥ ${path.readSymbolicLink()}"
        else
            extra.label = ""

        // TODO: move somewhere else
        assert(PosixFilePermissions.toString(attributes.permissions()) == posixPermissionsToString(attributes.permissions()))
    }
}