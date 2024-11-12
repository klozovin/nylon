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
    val targetFull get() = target as BaseTarget.TargetRegular

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

    fun tryCreateNewTarget(targetPath: Path): BaseTarget {
        try {
            val targetDirAttributes = targetPath.readAttributes<PosixFileAttributes>()
            val targetDirPermissions = targetDirAttributes.permissions()
            val targetDirEntry = Entry.EntryFull.of(targetPath, targetDirAttributes)
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
                            .map { (path, attrs) -> Entry.EntryFull.of(path, attrs) }
                            .sortedByDescending { it.path.isDirectory() }
                        BaseTarget.TargetRegular(targetDirEntry, entries)
                    }

                    //
                    // Can-read directory [r*-]: just list entries, but can't fetch any info about them
                    // (not even filetype).
                    //
                    targetDirPermissions.contains(OWNER_READ) -> {
                        val entries = targetPath.listDirectoryEntries().map { Entry.EntryBasic(it) }
                        BaseTarget.Restricted(targetDirEntry, entries)
                    }

                    //
                    // Can execute dir [-*x]: can access files inside by direct path, can add files to it.
                    //
                    targetDirPermissions.contains(OWNER_EXECUTE) -> {
                        BaseTarget.Unreadable(targetDirEntry)
                    }

                    //
                    // No read/execute permission, can't do anything with it.
                    //
                    else -> {
                        BaseTarget.Unreadable(targetDirEntry)
                    }
                }
                return newTarget
            } catch (e: AccessDeniedException) {
                return BaseTarget.Invalid(Entry.EntryBasic(targetPath), BaseTarget.Invalid.Error.ChangedWhileListing)
            } catch (e: NoSuchFileException) {
                return BaseTarget.Invalid(Entry.EntryBasic(targetPath), BaseTarget.Invalid.Error.ChangedWhileListing)
            }
        } catch (e: NoSuchFileException) {
            return BaseTarget.Invalid(Entry.EntryBasic(targetPath), BaseTarget.Invalid.Error.PathDoesNotExist)
        } catch (e: NotDirectoryException) {
            // TODO: List parent directory, select the file - maybe useful as a file picker later
            error("Unreachable")
        }
    }


    sealed class BaseTarget(open val entry: Entry) {
        open val isEmpty get() = true
        val path get() = entry.path

        /**
         * Directory does not exist, or is not a directory, or changed while listing
         */
        class Invalid(override val entry: Entry.EntryBasic, val error: Error) : BaseTarget(entry) {
            enum class Error {
                PathDoesNotExist,
                PathIsNotDirectory,
                ChangedWhileListing,
            }
        }

        // TODO: Maybe differentiate these two?
        /**
         * Directory can't be read: [-w-], [-wx]
         */
        class Unreadable(entry: Entry.EntryFull) : BaseTarget(entry)

        // [rw-]
        class Restricted(entry: Entry.EntryFull, val entries: List<Entry.EntryBasic>) : BaseTarget(entry) {
            // TODO: Maybe don't copy paste these properties?
            val count get() = entries.size
            override val isEmpty get() = entries.isEmpty()
            val isNotEmpty get() = entries.isNotEmpty()
        }

        // +r +x
        class TargetRegular(entry: Entry.EntryFull, val entries: List<Entry.EntryFull>) : BaseTarget(entry) {

            val count get() = entries.size
            override val isEmpty get() = entries.isEmpty()
            val isNotEmpty get() = entries.isNotEmpty()

            // TODO: Move up to another common abstract base class? Both Full and PathOnly should have this function
            fun indexOf(path: Path): Int = entries.indexOfFirst { it.path == path }
        }
    }


    sealed class Entry(val path: Path) {
        val name get() = path.name

        enum class Type {
            Block,
            Character,
            Directory,
            Pipe,
            Regular,
            Socket,
            Symlink,

            Other, // TODO: Delete, here only because Java can't identify files types besides
                   //       Regular, Directory, Symlink
        }


        class EntryBasic(path: Path) : Entry(path)


        class EntryFull(path: Path, val type: Type, val attributes: PosixFileAttributes) : Entry(path) {
            val owner = attributes.owner()
            val group = attributes.group()

            val permissions: Set<PosixFilePermission> = attributes.permissions()

            // BUG: Doesn't behave the same as path.isDirectory on symlinks, normalize that
            val isDirectory get() = attributes.isDirectory

            //        val isDirectory1 get() = Files.isDirectory()

            val isReadable get() = permissions.contains(OWNER_READ)

            fun linksToType(): Type {
                require(type == Type.Symlink)
                return when {
                    path.isDirectory() -> Type.Directory
                    path.isRegularFile() -> Type.Regular
                    path.isSymbolicLink() -> Type.Symlink
                    else -> TODO()
                }
            }

            companion object {
                fun of(path: Path): EntryFull =
                    of(path, path.readAttributes<PosixFileAttributes>())

                fun of(path: Path, attributes: PosixFileAttributes): EntryFull {
                    // TODO: What about other file types?
                    val entryType = when {
                        attributes.isDirectory -> Type.Directory
                        attributes.isRegularFile -> Type.Regular
                        attributes.isSymbolicLink -> Type.Symlink
                        else -> TODO()
                    }
                    return EntryFull(path, entryType, attributes)
                }
            }
        }
    }
}