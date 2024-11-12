package dewey

import dewey.BaseDirectoryEntry.DirectoryEntry
import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readAttributes

/**

# Naming

- directory, file, symlink, entries (directory contents), block, character, pipe

# List directory contents

- syscall
    - openat(2): O_RDONLY? O_NONBLOCK? O_CLOEXEC?
    - getdents(2): call repeatedly until returns zero/null
- libc
    - opendir(3)
    - readdir(3)
    - closedir(3)


# File attributes

- BasicFileAttributes
  - lastModified
  - lastAccess
  - creationTime
  - isDirectory
  - isRegularFile
  - isSymbolicLink
  - isOther
  - size
  - PosixFileAttributes
    - owner
    - group
    - permissions (PosixFilePermissions)
      - OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, ...
 */



// --------------------------------------------------------------------------------------------- //

// TODO: Is it okay to use data classes everywhere?

sealed class BaseDirectoryEntry {

    companion object {
        fun of(path: Path) =
            RestrictedEntry(path)

        fun of(path: Path, attrs: PosixFileAttributes): DirectoryEntry =
            when {
                attrs.isRegularFile -> File(path)
                attrs.isDirectory -> Directory(path)
                attrs.isSymbolicLink -> Symlink(path)
                attrs.isOther -> Other(path)
                else -> error("Unreachable")
            }
    }

    data class RestrictedEntry(val path: Path) : BaseDirectoryEntry()

    sealed class DirectoryEntry : BaseDirectoryEntry()
}

data class File(val path: Path) : DirectoryEntry()
data class Directory(val path: Path) : DirectoryEntry()
data class Symlink(val path: Path) : DirectoryEntry()
data class Other(val path: Path) : DirectoryEntry() // TODO: Delete. Used because Java can't check for other file types


// --------------------------------------------------------------------------------------------- //

sealed class DirectoryListingResult {
    data class Error(val err: Type) : DirectoryListingResult() {
        enum class Type {
            AccessDenied,
            PathNonExistent,
            PathNotDirectory,
            ChangedWhileReading,
        }
    }

    data class Listing(val entries: List<DirectoryEntry>) : DirectoryListingResult()
    data class RestrictedListing(val entries: List<BaseDirectoryEntry.RestrictedEntry>) : DirectoryListingResult()
}


fun readDirectory(path: Path): DirectoryListingResult {
    try {
        val dirAttributes = path.readAttributes<PosixFileAttributes>()
        val dirPermissions = dirAttributes.permissions()

        println("Reading: [${path}]")
        println("Attributes: $dirAttributes")
        println("Permissions: $dirPermissions")

        try {
            val entries = path.listDirectoryEntries()
            try {
                val directoryEntries = entries
                    .parallelStream()
                    .map {
                        val attributes = it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
                        BaseDirectoryEntry.of(it, attributes)
                    }
                    .toList()
                    .sortedByDescending { it is DirectoryEntry }
                return DirectoryListingResult.Listing(directoryEntries)
            } catch (e: AccessDeniedException) {
                // TODO: Possible race condition: this can be raised in two ways:
                //       1. listing a -x directory, handled here
                //       2. directory permissions get changed to -r while listing, not handled (should return CWR err)
                //          Possible fix: re-listDirectoryEntries? or retry this entire function? stackoverflow?
                val restrictedEntries = entries.map { BaseDirectoryEntry.of(it) }
                return DirectoryListingResult.RestrictedListing(restrictedEntries)
            } catch (e: NoSuchFileException) {
                return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.ChangedWhileReading)
            }
        } catch (e: NotDirectoryException) {
            return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.PathNotDirectory)
        }
    } catch (e: AccessDeniedException) {
        return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.AccessDenied)
    } catch (e: NoSuchFileException) {
        return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.PathNonExistent)
    }
}

// --------------------------------------------------------------------------------------------- //


class FilesystemNavigatorMax {
    lateinit var working: DirectoryListingResult

    fun navigateTo(path: Path) {
        working = readDirectory(path)
    }
}



// --------------------------------------------------------------------------------------------- //

fun main(args: Array<String>) {
    val targetDirectory = args.getOrElse(0) { "." }
    val entries = readDirectory(Path.of(targetDirectory))
    println(entries)
}