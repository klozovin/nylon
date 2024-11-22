package dewey

import dewey.NullableSubjectDelegate.Companion.asObservable
import dewey.SubjectDelegate.Companion.asObservable
import dewey.fsnav.BaseDirectoryEntry
import dewey.fsnav.BaseDirectoryEntry.DirectoryEntry
import dewey.fsnav.BaseDirectoryEntry.RestrictedEntry
import dewey.fsnav.DirectoryListingResult
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
        DirectoryBrowserState::cwdListing.asObservable().subscribe(::onCwdListingChange)
        DirectoryBrowserState::selectedItem.asObservable().subscribe(::onSelectedItemChange)
    }

    private fun onSelectedItemChange(selectedItem: Maybe<BaseDirectoryEntry>) {
        when (selectedItem) {
            is Maybe.Just -> when (val selected = selectedItem.value) {
                is RestrictedEntry -> updateFocused(selected.path)
                is DirectoryEntry -> updateFocused(selected.path)
            }

            is Maybe.None -> clearFocused()
        }

    }

    private fun onCwdListingChange(cwd: DirectoryListingResult) {
        // Don't show double "//" when showing the root filesystem, because `pathString` doesn't have a trailing `/`.
        val path = cwd.path
        current.label = "${path.pathString}${if (path.parent != null) "/" else ""}"
    }

    private fun updateFocused(path: Path) {
        if (path.fileName == null) {
            error("cant be here")
        }
        focused.label = path.fileName?.pathString ?: "—"
    }

    private fun clearFocused() {
        focused.label = "—"
    }
}