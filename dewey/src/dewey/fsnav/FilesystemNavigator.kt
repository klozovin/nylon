package dewey.fsnav

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.util.logging.Logger
import kotlin.io.path.*


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

val LOG: Logger = Logger.getLogger(FilesystemNavigator::class.qualifiedName)


// --------------------------------------------------------------------------------------------- //

// TODO: Is it okay to use data classes everywhere?


// --------------------------------------------------------------------------------------------- //

// TODO: Bad name? Can't guarantee we're given a directory, so what to do?
fun readDirectory(path: Path): DirectoryListingResult {
    try {
        val dirAttributes = path.readAttributes<PosixFileAttributes>()
        val dirPermissions = dirAttributes.permissions()

        println("Reading: [${path}]")
        println("Attributes: $dirAttributes")
        println("Permissions: $dirPermissions")

        try {
            val entries = path.listDirectoryEntries()
            val directoryEntry = BaseDirectoryEntry.directoryOf(path, dirAttributes)

            try {
                val directoryEntries = entries
                    .parallelStream()
                    .map { entryPath ->
                        // TODO: Resolve symlinks here: first read with NOFOLLOW, then follow and see where it points to
                        val attributes = entryPath.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
                        BaseDirectoryEntry.of(entryPath, attributes)
                    }
                    .toList()
                    .sortedBy { it !is Directory }

                // MUST check this!
                // We differentiate regular/restricted directories by the exception thrown when trying to read
                // attributes of its entries. When the directory is empty, there's no opportunity for the exception to
                // get thrown, so have to do it like this.
                if (directoryEntries.isNotEmpty()) {
                    // Found directory entries => directory is not empty, and is +r+x (otherwise we would be in a catch
                    // clause).
                    require(path.isReadable() && path.isExecutable())
                    return DirectoryListingResult.Listing(directoryEntry, directoryEntries)
                } else {
                    // Directory is empty, but is it +r+x or +r-x?
                    require(path.isReadable())
                    return if (path.isExecutable())
                        DirectoryListingResult.Listing(directoryEntry, emptyList())
                    else
                        DirectoryListingResult.RestrictedListing(directoryEntry, emptyList())
                }
            } catch (e: AccessDeniedException) {
                // TODO: Possible race condition: This exception can be thrown in two ways:
                //       1. listing a -x directory, ok handled here
                //       2. directory permissions get changed to -r while listing, not handled (should return CWR err)
                //          Possible fix: re-listDirectoryEntries? or retry this entire function? stackoverflow? test?
                require(!path.isExecutable())
                val restrictedEntries = entries.map { BaseDirectoryEntry.of(it) }
                return DirectoryListingResult.RestrictedListing(directoryEntry, restrictedEntries)
            } catch (e: NoSuchFileException) {
                return DirectoryListingResult.Error(path, DirectoryListingResult.Error.Type.ChangedWhileReading)
            }
        } catch (e: NotDirectoryException) {
            return DirectoryListingResult.Error(path, DirectoryListingResult.Error.Type.PathNotDirectory)
        } catch (e: AccessDeniedException) {
            require(!path.isReadable())
            println("Access denied to: <target>")
            return DirectoryListingResult.Error(path, DirectoryListingResult.Error.Type.AccessDenied)
        }
    } catch (e: AccessDeniedException) {
        println("Access denied to: <target.parent>")
        return DirectoryListingResult.Error(path, DirectoryListingResult.Error.Type.AccessDenied)
    } catch (e: NoSuchFileException) {
        return DirectoryListingResult.Error(path, DirectoryListingResult.Error.Type.PathNonExistent)
    }
}

// --------------------------------------------------------------------------------------------- //


class FilesystemNavigator {
    //    lateinit var workingPath: Path
    lateinit var working: DirectoryListingResult
    var previousWorking: Path? = null

    val isInitialized get() = ::working.isInitialized

    fun navigateTo(path: Path) {
        if (::working.isInitialized) previousWorking = working.path
        working = readDirectory(path)

//        check(previousWorking != working.path)
    }

//    fun navigateToParent() {
//        val parent = workingPath.parent ?: return
//        working = readDirectory(parent)
//        workingPath = parent
//    }

    fun reload() {
        working = readDirectory(working.path)
    }
}


// --------------------------------------------------------------------------------------------- //

fun main(args: Array<String>) {
    val targetDirectory = args.getOrElse(0) { "." }
    val entries = readDirectory(Path.of(targetDirectory))
    println(entries)
}