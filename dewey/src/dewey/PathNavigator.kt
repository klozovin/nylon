package dewey

import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.util.logging.Logger
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readAttributes

val LOG: Logger = Logger.getLogger(PathNavigator::class.qualifiedName)

/*

Possible states:
- Valid path
    - Directory with items
    - Empty directory
    - No permission to list

- Invalid path (directory not existing)

:THINK: Should it be possible to create a PathNavigator with invalid target?

TODO: Find all possible error states, how to represent them and recover if possible?
TODO: Should we keep focus history here or in the widget?
 */

class PathNavigator {
    lateinit var target: Directory

    /**
     * Reload the contents of [target]. As if we're navigating there for the first time.
     */
    fun reload() {
        // TODO: What if the target doesn't exist anymore?
        // TODO: What if the target is not readable anymore?

    }

    /**
     * Navigate to directory.
     *
     * @throws TODO
     */
    fun navigateTo(path: Path) {
        // MAYBE: Is this the best place to handle these requirement? And in this way? Even if handled here, we could
        //        still error out down the stack, when opening a directory or listing it.
        // TODO: Remove these checks later, they don't add anything except one more exception to catch.
        require(path.exists())
        require(path.isDirectory())

        // TODO: What about exceptions?
        target = Directory(path)
    }

    /**
     * Go to the parent directory of the current target.
     */
    fun navigateToParent() {
        LOG.info("Parent of <$target> is null, doing nothing")
        if (target.path.parent != null)
            return
        navigateTo(target.path.parent)
    }


    /**
     * Snapshot of a single directory at some point in the past. Contains it's listing and attributes for everything
     * contained in it.
     */
    class Directory(ofPath: Path) {

        val path: Path

        /**
         * Contents of the directory. Order unspecified.
         */
        val entries: List<Entry>

        val isEmpty: Boolean

        /**
         * Number of entries in the directory.
         */
        val count: Int

        init {
            path = ofPath

            // Throws NoSuchFileException (target no long exists), NotDirectoryException
            val entries = ofPath.listDirectoryEntries()

            // Throws: NoSuchFileException (target missing, something inside target missing)
            val entriesAttributes = entries.parallelStream().map {
                it.readAttributes<PosixFileAttributes>(LinkOption.NOFOLLOW_LINKS)
            }.toList()

            this.entries = (entries zip entriesAttributes).map(::Entry).sortedByDescending { it.isDirectory }

            isEmpty = this.entries.isEmpty()
            count = this.entries.size
        }

        /**
         * Return the index of the Entry with the given path.
         */
        fun indexOf(path: Path): Int {
            return entries.indexOfFirst { it.path == path }
        }
    }


    /**
     * Directory entry: file, directory, symbolic link, .... Combine Path with PosixFileAttributes for easier handling.
     */
    data class Entry(val path: Path, val attributes: PosixFileAttributes) {

        constructor(p: Pair<Path, PosixFileAttributes>) : this(p.first, p.second)

        val isDirectory get() = attributes.isDirectory
        val isRegularFile get() = attributes.isRegularFile
        val isSymbolicLink get() = attributes.isSymbolicLink
        val isReadable get() = attributes.permissions().contains(PosixFilePermission.OWNER_READ)
    }
}