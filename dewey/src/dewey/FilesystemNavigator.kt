package dewey

import dewey.BaseDirectoryEntry.DirectoryEntry
import java.nio.file.*
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.UserPrincipal
import java.util.logging.Logger
import kotlin.io.path.*
import kotlin.reflect.KClass


val LOG: Logger = Logger.getLogger(FilesystemNavigator::class.qualifiedName)

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

        fun of(path: Path, attrs: PosixFileAttributes): DirectoryEntry {
            val constructor = when {
                attrs.isRegularFile -> ::File
                attrs.isDirectory -> ::Directory
                attrs.isSymbolicLink -> ::Symlink
                attrs.isOther -> ::Other
                else -> error("UNREACHABLE")
            }
            return constructor(path, attrs.owner(), attrs.group(), attrs, attrs.permissions())
        }
    }

    data class RestrictedEntry(
        val path: Path
    ) : BaseDirectoryEntry()

    sealed class DirectoryEntry(
        open val path: Path,
        open val owner: UserPrincipal,      // TODO: Remove, use .attributes.owner
        open val group: GroupPrincipal,     // TODO: Remove, use .attributes.group
        open val attributes: PosixFileAttributes,
        open val permissions: Set<PosixFilePermission>
    ) : BaseDirectoryEntry() {
        //
    }
}

data class File(
    override val path: Path,
    override val owner: UserPrincipal,
    override val group: GroupPrincipal,
    override val attributes: PosixFileAttributes,
    override val permissions: Set<PosixFilePermission>
) : DirectoryEntry(path, owner, group, attributes, permissions)

data class Directory(
    override val path: Path,
    override val owner: UserPrincipal,
    override val group: GroupPrincipal,
    override val attributes: PosixFileAttributes,
    override val permissions: Set<PosixFilePermission>
) : DirectoryEntry(path, owner, group, attributes, permissions)

data class Symlink(
    override val path: Path,
    override val owner: UserPrincipal,
    override val group: GroupPrincipal,
    override val attributes: PosixFileAttributes,
    override val permissions: Set<PosixFilePermission>
) : DirectoryEntry(path, owner, group, attributes, permissions) {

    fun linksToType(): String {
        // TODO: Horrible
        return when {
            path.isDirectory() -> "d"
            path.isRegularFile() -> "f"
            path.isSymbolicLink() -> "l"
            else -> TODO()
        }
    }

    // TODO: Does this even work? Horrible as well
    fun linksToClass(): KClass<out DirectoryEntry> {
        return when {
            path.isDirectory() -> Directory::class
            path.isRegularFile() -> File::class
            else -> TODO()
        }
    }
}

data class Other( // TODO: Delete. Used because Java can't check for other file types
    override val path: Path,
    override val owner: UserPrincipal,
    override val group: GroupPrincipal,
    override val attributes: PosixFileAttributes,
    override val permissions: Set<PosixFilePermission>
) : DirectoryEntry(path, owner, group, attributes, permissions)


// --------------------------------------------------------------------------------------------- //

sealed class DirectoryListingResult {

    // TODO: Add field for directory DirectoryEntry
    data class Listing(val entries: List<DirectoryEntry>) : DirectoryListingResult() {
        val count = entries.size
        val isEmpty = entries.isEmpty()
        val isNotEmpty = entries.isNotEmpty()
    }

    data class RestrictedListing(val entries: List<BaseDirectoryEntry.RestrictedEntry>) : DirectoryListingResult() {
        val count = entries.size
        val isNotEmpty = entries.isNotEmpty()
    }

    data class Error(val err: Type) : DirectoryListingResult() {
        enum class Type {
            AccessDenied,
            PathNonExistent,
            PathNotDirectory,
            ChangedWhileReading,
        }
    }
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
                    return DirectoryListingResult.Listing(directoryEntries)
                } else {
                    // Directory is empty, but is it +r+x or +r-x?
                    require(path.isReadable())
                    return if (path.isExecutable())
                        DirectoryListingResult.Listing(emptyList())
                    else
                        DirectoryListingResult.RestrictedListing(emptyList())
                }
            } catch (e: AccessDeniedException) {
                // TODO: Possible race condition: This exception can be thrown in two ways:
                //       1. listing a -x directory, ok handled here
                //       2. directory permissions get changed to -r while listing, not handled (should return CWR err)
                //          Possible fix: re-listDirectoryEntries? or retry this entire function? stackoverflow? test?
                require(!path.isExecutable())
                val restrictedEntries = entries.map { BaseDirectoryEntry.of(it) }
                return DirectoryListingResult.RestrictedListing(restrictedEntries)
            } catch (e: NoSuchFileException) {
                return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.ChangedWhileReading)
            }
        } catch (e: NotDirectoryException) {
            return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.PathNotDirectory)
        } catch (e: AccessDeniedException) {
            require(!path.isReadable())
            println("Access denied to: <target>")
            return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.AccessDenied)
        }
    } catch (e: AccessDeniedException) {
        println("Access denied to: <target.parent>")
        return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.AccessDenied)
    } catch (e: NoSuchFileException) {
        return DirectoryListingResult.Error(DirectoryListingResult.Error.Type.PathNonExistent)
    }
}

// --------------------------------------------------------------------------------------------- //


class FilesystemNavigator {
    var previousWorkingPath: Path? = null
    lateinit var workingPath: Path
    lateinit var working: DirectoryListingResult

    val isInitialized get() = ::working.isInitialized

    fun navigateTo(path: Path) {
        if (::workingPath.isInitialized) previousWorkingPath = workingPath
        working = readDirectory(path)
        workingPath = path
    }

//    fun navigateToParent() {
//        val parent = workingPath.parent ?: return
//        working = readDirectory(parent)
//        workingPath = parent
//    }

    fun reload() {
        working = readDirectory(workingPath)
    }
}


// --------------------------------------------------------------------------------------------- //

fun main(args: Array<String>) {
    val targetDirectory = args.getOrElse(0) { "." }
    val entries = readDirectory(Path.of(targetDirectory))
    println(entries)
}