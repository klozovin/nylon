package dewey

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.util.logging.Logger
import kotlin.io.path.*

val LOG: Logger = Logger.getLogger(FilesystemNavigator::class.qualifiedName)

/*

Possible states:
- Valid path
    - Directory with items
    - Empty directory
    - No permission to list

- Invalid path (directory not existing)

:THINK: Should it be possible to create a PathNavigator with invalid target?
:TODO: Find all possible error states, how to represent them and recover if possible?
:TODO: Should we keep focus history here or in the widget?
 */

class FilesystemNavigator {

    lateinit var target: BaseTarget

    @Deprecated("Immediately")
    val targetFull get() = target as BaseTarget.TargetFull

    // TODO: Delete
    fun setTargetTo(newTarget: BaseTarget) {
        LOG.info("Setting FilesystemNavigator.target to [${newTarget.path}]")
        target = newTarget
    }

    fun navigateTo(path: Path) {
        setTargetTo(tryCreateNewTarget(path))
    }

    fun navigateToParent() {
        LOG.info("Parent of <${target.path}> is [${target.path.parent}], doing nothing")
        if (target.path.parent == null)
            return // Skip when can no longer go up the tree

        navigateTo(target.path.parent)
    }

    fun reload() {
        setTargetTo(tryCreateNewTarget(target.path))
    }


    /**
     * @throws PathDoesNotExistException
     * @throws PathIsNotDirectoryException
     * @throws DirectoryChangedWhileListingException
     */
    private fun tryCreateNewTarget(targetPath: Path): BaseTarget {

        /**
         * Try reading attributes. Possible ways to fail:
         * - don't have permission to read attributes: TODO: test this, can this happen inside a rw- dir?
         */
        try {
            val targetDirAttributes = targetPath.readAttributes<PosixFileAttributes>()
            val targetDirPermissions = targetDirAttributes.permissions()
            val targetDirEntry = EntryFull.of(targetPath, targetDirAttributes)

            try {
                val newTarget = when {
                    //
                    // Regular directory [r*x]: can list entries, can fetch attributes for entries.
                    //
                    targetDirPermissions.containsAll(listOf(OWNER_READ, OWNER_EXECUTE)) -> {
                        val dirPathEntries = targetPath.listDirectoryEntries()
                        val entryAttributes = dirPathEntries
                            .parallelStream()
                            .map { it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS) }
                            .toList()
                        val entries = (dirPathEntries zip entryAttributes)
                            // TODO: Maybe run in parallel?
                            .map { (path, attrs) -> EntryFull.of(path, attrs) }
                            .sortedByDescending { it.path.isDirectory() }
                        BaseTarget.TargetFull(targetDirEntry, entries)
                    }

                    //
                    // Can-read directory [r*-]: just list entries, but can't fetch any info about them
                    // (not even filetype).
                    //
                    targetDirPermissions.contains(OWNER_READ) -> {
                        val entries = targetPath.listDirectoryEntries().map { EntryBasic(it) }
                        BaseTarget.TargetPathsOnly(targetDirEntry, entries)
                    }

                    //
                    // Can execute dir [-*x]: can access files inside by direct path, can add files to it.
                    //
                    targetDirPermissions.contains(OWNER_EXECUTE) -> {
                        BaseTarget.TargetNotAccessible(targetDirEntry)
                    }

                    //
                    // No read/execute permission, can't do anything with it.
                    //
                    else -> {
                        BaseTarget.TargetNotAccessible(targetDirEntry)
                    }
                }
                return newTarget
            } catch (e: AccessDeniedException) {
                // TODO: Delete this codepath
                // TODO: What to do here?
                throw DirectoryChangedWhileListingException(e)
            } catch (e: NoSuchFileException) {
                error("Unreachable")
                // TODO: Delete this codepath

                // TODO: What do to here? And how did we get here.
                throw DirectoryChangedWhileListingException(e)
            }
        } catch (e: NoSuchFileException) {
            // We were given a Path that does not exist, nothing we can do now about it.
            // Don't change the `target`.
            // TODO: Delete this codepath
            error("Unreachable")
            throw PathDoesNotExistException()
        } catch (e: NotDirectoryException) {
            // Path given is not a directory, can't list it
            // TODO: Delete this codepath
            error("Unreachable")
            throw PathIsNotDirectoryException()
        }
    }

    //
    // Local exceptions
    //
    class CantListEntriesException : Exception()
    class PathDoesNotExistException : Exception()
    class PathIsNotDirectoryException : Exception()
    class DirectoryChangedWhileListingException(val e: Exception) : Exception()


    sealed class BaseTarget(val entry: EntryFull) {

        val path get() = entry.path

        class TargetInvalid(entry: EntryFull, val error: Error) : BaseTarget(entry) {
            enum class Error {
                PathDoesNotExist,
                PathIsNotDirectory,
                ChagnedWhileListing,
            }
        }

        // TODO: Maybe differentiate these two?
        // -w-, -wx
        open class TargetNotAccessible(entry: EntryFull) : BaseTarget(entry)

        // [rw-]
        class TargetPathsOnly(entry: EntryFull, val entries: List<EntryBasic>) : BaseTarget(entry)

        // [rwx]
        class TargetFull(entry: EntryFull, val entries: List<EntryFull>) : BaseTarget(entry) {

            val count get() = entries.size
            val isEmpty get() = entries.isEmpty()
            val isNotEmpty get() = entries.isNotEmpty()

            // TODO: Move up to another common abstract base class? Both Full and PathOnly should have this function
            fun indexOf(path: Path): Int = entries.indexOfFirst { it.path == path }
        }
    }

    enum class EntryType {
        Regular,
        Block,
        Character,
        Directory,
        Symlink,
        Pipe,
        Socket,
        Door,
        Other,
    }


    sealed class Entry(val path: Path) {
        val name get() = path.name
    }

    class EntryBasic(path: Path) : Entry(path)


    class EntryFull(path: Path, val type: EntryType, val attributes: PosixFileAttributes) : Entry(path) {

        val owner = attributes.owner()
        val group = attributes.group()
        val permissions: Set<PosixFilePermission> = attributes.permissions()

        // BUG: Doesn't behave the same as path.isDirectory on symlinks, normalize that
        val isDirectory get() = attributes.isDirectory
//        val isDirectory1 get() = Files.isDirectory()

        val isReadable get() = permissions.contains(OWNER_READ)

        fun linksToType(): EntryType {
            require(type == EntryType.Symlink)
            return when {
                path.isDirectory() -> EntryType.Directory
                path.isRegularFile() -> EntryType.Regular
                path.isSymbolicLink() -> EntryType.Symlink
                else -> TODO()
            }
        }

        companion object {
            fun of(path: Path): EntryFull =
                of(path, path.readAttributes<PosixFileAttributes>())

            fun of(path: Path, attributes: PosixFileAttributes): EntryFull {
                // TODO: What about other file types?
                val entryType = when {
                    attributes.isDirectory -> EntryType.Directory
                    attributes.isRegularFile -> EntryType.Regular
                    attributes.isSymbolicLink -> EntryType.Symlink
                    else -> TODO()
                }
                return EntryFull(path, entryType, attributes)
            }
        }
    }
}


// TODO: Is this monstrosity possibly useful? Could help with "!!" in UI code all over the place

//open class EntryPath(val path: Path) {
//
//    abstract class EntryPathAttributes(path: Path, val attributes: PosixFileAttributes) : EntryPath(path) {
//
//        open class Directory(path: Path, attributes: PosixFileAttributes) : EntryPathAttributes(path, attributes) {
//
//            open class DirectoryListing(
//                path: Path,
//                attributes:
//                PosixFileAttributes,
//                entries: List<EntryPath>
//            ) : Directory(path, attributes) {
//
//                class DirectoryListingAttributes(
//                    path: Path,
//                    attributes: PosixFileAttributes,
//                    entries: List<EntryPathAttributes>
//                ) : DirectoryListing(path, attributes, entries)
//            }
//        }
//
//        class File(path: Path, attributes: PosixFileAttributes) : EntryPathAttributes(path, attributes)
//
//        class Symlink(path: Path, attributes: PosixFileAttributes) : EntryPathAttributes(path, attributes)
//    }
//}