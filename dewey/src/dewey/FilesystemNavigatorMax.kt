package dewey

import dewey.BaseDirectoryEntry.DirectoryEntry
import java.nio.file.*
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.UserPrincipal
import kotlin.io.path.*
import kotlin.reflect.KClass

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
        open val owner: UserPrincipal,
        open val group: GroupPrincipal,
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
    data class Error(val err: Type) : DirectoryListingResult() {
        enum class Type {
            AccessDenied,
            PathNonExistent,
            PathNotDirectory,
            ChangedWhileReading,
        }
    }

    data class Listing(val entries: List<DirectoryEntry>) : DirectoryListingResult() {
        val count = entries.size
        val isEmpty = entries.isEmpty()
        val isNotEmpty = entries.isNotEmpty()
    }

    data class RestrictedListing(val entries: List<BaseDirectoryEntry.RestrictedEntry>) : DirectoryListingResult() {
        val count = entries.size
        val isNotEmpty = entries.isNotEmpty()
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
                    .map {
                        val attributes = it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
                        BaseDirectoryEntry.of(it, attributes)
                    }
                    .toList()
                    .sortedBy { it !is Directory }
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
    lateinit var workingPath: Path
    lateinit var working: DirectoryListingResult

    fun navigateTo(path: Path) {
        working = readDirectory(path)
        workingPath = path
    }

    fun navigateToParent() {
        val parent = workingPath.parent ?: return
        working = readDirectory(parent)
        workingPath = parent
    }
}


// --------------------------------------------------------------------------------------------- //

fun main(args: Array<String>) {
    val targetDirectory = args.getOrElse(0) { "." }
    val entries = readDirectory(Path.of(targetDirectory))
    println(entries)
}