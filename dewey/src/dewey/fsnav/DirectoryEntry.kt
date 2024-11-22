package dewey.fsnav

import dewey.fsnav.BaseDirectoryEntry.DirectoryEntry
import dewey.unreachable
import java.nio.file.Path
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.UserPrincipal
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink
import kotlin.reflect.KClass

sealed interface BaseDirectoryEntry {

    data class RestrictedEntry(
        val path: Path
    ) : BaseDirectoryEntry

    sealed class DirectoryEntry(
        open val path: Path,
        open val attributes: PosixFileAttributes,
        open val permissions: Set<PosixFilePermission>
    ) : BaseDirectoryEntry {
        val owner: UserPrincipal get() = attributes.owner()
        val group: GroupPrincipal get() = attributes.group()
    }

    companion object {

        fun of(path: Path) =
            RestrictedEntry(path)

        fun of(path: Path, attrs: PosixFileAttributes): DirectoryEntry {
            val constructor = when {
                attrs.isRegularFile -> ::File
                attrs.isDirectory -> ::Directory
                attrs.isSymbolicLink -> ::Symlink
                attrs.isOther -> ::Other
                else -> unreachable()
            }
            return constructor(path, attrs)
        }

        fun directoryOf(path: Path, attrs: PosixFileAttributes): Directory {
            val dir = of(path, attrs)
            check(dir is Directory)
            return dir
        }
    }
}

data class File(
    override val path: Path,
    override val attributes: PosixFileAttributes,
) : DirectoryEntry(path, attributes, attributes.permissions())

data class Directory(
    override val path: Path,
    override val attributes: PosixFileAttributes,
) : DirectoryEntry(path, attributes, attributes.permissions())

data class Symlink(
    override val path: Path,
    override val attributes: PosixFileAttributes,
) : DirectoryEntry(path, attributes, attributes.permissions()) {

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

// TODO: Delete. Used because Java can't check for other file types
data class Other(
    override val path: Path,
    override val attributes: PosixFileAttributes,
) : DirectoryEntry(path, attributes, attributes.permissions())

