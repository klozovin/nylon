package dewey

import dewey.NullableSubjectDelegate.Companion.asObservable
import dewey.fsnav.*
import dewey.fsnav.BaseDirectoryEntry.DirectoryEntry
import dewey.fsnav.BaseDirectoryEntry.RestrictedEntry
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
        DirectoryBrowserState::selectedItem.asObservable().subscribe(::onSelectedItemChange)
    }

    private fun onSelectedItemChange(selectedItem: Maybe<BaseDirectoryEntry>) {
        when (selectedItem) {
            is Maybe.Just -> when (val selected = selectedItem.value) {
                is RestrictedEntry -> clear()
                is DirectoryEntry -> update(selected)
            }

            is Maybe.None -> clear()
        }
    }

    private fun update(entry: DirectoryEntry) {
        // Permissions string [rwx]
        prefix.label = when (entry) {
            is Directory -> "/"
            is File -> "-" // TODO: add "*" if executable
            is Symlink -> "@"
            is Other -> "-"
            // TODO: The rest, | -> pipe, = -> socket,
        }
        permissions.label = PosixFilePermissions
            .toString(entry.permissions)
            .chunked(3)
            .joinToString(separator = ".")
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

    private fun clear() {
        prefix.label = "—"
        permissions.label = "—"
        owner.label = "—"
        group.label = "—"
        size.label = "—"
        modifiedAt.label = "—"
        extra.label = ""
    }
}