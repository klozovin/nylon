package dewey

import dewey.NullableSubjectDelegate.Companion.asObservable
import dewey.fsnav.BaseDirectoryEntry
import dewey.fsnav.File
import org.gnome.gtk.Picture
import kotlin.io.path.extension


class WidgetImageView {
    val widget = Picture()


    init {
        DirectoryBrowserState::selectedItem.asObservable().subscribe(::onSelectedItemChange)
    }


    private fun onSelectedItemChange(selectedItem: Maybe<BaseDirectoryEntry>) {
        if (selectedItem is Maybe.Just && selectedItem.value is File) {
            val extension = selectedItem.value.path.extension.lowercase()
            if (extension == "jpg" || extension == "jpeg" || extension == "png")
                widget.setFilename(selectedItem.value.path.toString())
            else
                widget.setFilename(null)
        }
    }
}