package dewey.fsnav

import dewey.fsnav.BaseDirectoryEntry.DirectoryEntry
import dewey.fsnav.BaseDirectoryEntry.RestrictedEntry
import java.nio.file.Path

sealed class DirectoryListingResult(
    open val path: Path
) {

    sealed class ListingBase(
        val directory: Directory,
        open val entries: List<BaseDirectoryEntry>
    ) : DirectoryListingResult(directory.path) {
        val count get() = entries.size
        val isEmpty get() = entries.isEmpty()
        val isNotEmpty get() = entries.isNotEmpty()
    }

    class Listing(
        directory: Directory,
        override val entries: List<DirectoryEntry>
    ) : ListingBase(directory, entries)

    class RestrictedListing(
        directory: Directory,
        override val entries: List<RestrictedEntry>
    ) : ListingBase(directory, entries)

    data class Error(override val path: Path, val err: Type) : DirectoryListingResult(path) {
        enum class Type {
            AccessDenied,
            PathNonExistent,
            PathNotDirectory,
            ChangedWhileReading,
        }
    }
}