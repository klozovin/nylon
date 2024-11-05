package dewey

import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.util.logging.Logger
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readAttributes

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
    lateinit var target: Target

    fun navigateTo(path: Path) {
        target = tryCreateNewTarget(path)
    }

    fun navigateToParent() {
        LOG.info("Parent of <$target> is null, doing nothing")
        if (target.path.parent != null)
            return
        navigateTo(target.path.parent)
    }

    fun reload() {
        target = tryCreateNewTarget(target.path)
    }


    /**
     * @throws PathDoesNotExistException
     * @throws PathIsNotDirectoryException
     * @throws DirectoryChangedWhileListingException
     */
    private fun tryCreateNewTarget(targetPath: Path): Target {

        /**
         * Try reading attributes. Possible ways to fail:
         * - don't have permission to read attributes: TODO: test this, can this happen inside a rw- dir?
         */
        try {
            val targetDirAttributes = targetPath.readAttributes<PosixFileAttributes>()
            val targetDirPermissions = targetDirAttributes.permissions()
            val targetDirEntry =
                Entry(targetPath, Entry.Type.Directory, targetDirAttributes)

            try {
                val newTarget = when {
                    //
                    // Regular directory: can list entries, can fetch attributes for entries.
                    //
                    targetDirPermissions.containsAll(listOf(OWNER_READ, OWNER_EXECUTE)) -> {
                        val dirPathEntries = targetPath.listDirectoryEntries()
                        val entryAttributes = dirPathEntries
                            .parallelStream()
                            .map {
                                it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
                            }
                            .toList()
                        val entries = (dirPathEntries zip entryAttributes)
                            .map { (path, attrs) ->
                                // TODO: Maybe run in parallel?
                                // TODO: Find other types as well
                                val entryType = when {
                                    attrs.isRegularFile -> Entry.Type.Regular
                                    attrs.isDirectory -> Entry.Type.Directory
                                    attrs.isSymbolicLink -> Entry.Type.Symlink
                                    else -> Entry.Type.Other
                                }
                                Entry(path, entryType, attrs)
                            }
                            .sortedByDescending { it.isDirectory }
                        Target(targetDirEntry, entries)
                    }

                    //
                    // Can-read directory: just list entries, but can't fetch any info about them (not even filetype)
                    //
                    targetDirPermissions.contains(OWNER_READ) -> {
                        val dirPathEntries = targetPath.listDirectoryEntries().map { Entry.ofPath(it) }
                        Target(targetDirEntry, dirPathEntries)
                    }

                    //
                    // Can execute dir: can access files inside by direct path
                    //
                    targetDirPermissions.contains(OWNER_EXECUTE) -> {
                        Target(targetDirEntry, null)
                    }

                    //
                    // No read/execute permission, can't do anything with it.
                    //
                    else -> {
                        Target(targetDirEntry, null)
                    }
                }
                return newTarget
            } catch (e: AccessDeniedException) {
                // TODO: What to do here?
                throw DirectoryChangedWhileListingException(e)
            } catch (e: NoSuchFileException) {
                // TODO: What do to here? And how did we get here.
                throw DirectoryChangedWhileListingException(e)
            }
        } catch (e: NoSuchFileException) {
            // We were given a Path that does not exist, nothing we can do now about it.
            // Don't change the `target`.
            throw PathDoesNotExistException()
        } catch (e: NotDirectoryException) {
            // Path given is not a directory, can't list it
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


    class Target(val entry: Entry, val entries: List<Entry>?) {
        val path get() = entry.path
        val type get() = entry.type!!
        val count get() = entries!!.size
        val isEmpty get() = entries!!.isEmpty()

        fun indexOf(path: Path): Int = entries!!.indexOfFirst { it.path == path }

        init {
            require(entry.type == Entry.Type.Directory)
        }
    }


    class Entry(val path: Path, val type: Type?, val attributes: PosixFileAttributes?) {

        val owner = attributes?.owner()
        val group = attributes?.group()
        val permissions: Set<PosixFilePermission>? = attributes?.permissions()

        enum class Type {
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

        val isDirectory get() = attributes!!.isDirectory
        val isRegularFile get() = attributes!!.isRegularFile
        val isSymbolicLink get() = attributes!!.isSymbolicLink
        val isReadable get() = attributes!!.permissions().contains(OWNER_READ)

        fun linksToType(): Type {
            TODO() // if symlink, return what it links to
        }

        companion object {
            fun ofPath(path: Path) = Entry(path, null, null)
        }
    }
}


// TODO: Is this monstrosity possibly useful? Could help with "!!" in UI code all over the place

open class EntryPath(val path: Path) {

    abstract class EntryPathAttributes(path: Path, val attributes: PosixFileAttributes) : EntryPath(path) {

        open class Directory(path: Path, attributes: PosixFileAttributes) : EntryPathAttributes(path, attributes) {

            open class DirectoryListing(
                path: Path,
                attributes:
                PosixFileAttributes,
                entries: List<EntryPath>
            ) : Directory(path, attributes) {

                class DirectoryListingAttributes(
                    path: Path,
                    attributes: PosixFileAttributes,
                    entries: List<EntryPathAttributes>
                ) : DirectoryListing(path, attributes, entries)
            }
        }

        class File(path: Path, attributes: PosixFileAttributes) : EntryPathAttributes(path, attributes)

        class Symlink(path: Path, attributes: PosixFileAttributes) : EntryPathAttributes(path, attributes)
    }
}