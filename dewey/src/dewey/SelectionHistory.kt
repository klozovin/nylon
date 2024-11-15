package dewey

import java.nio.file.Path


/**
 * When leaving a directory, save the currently focused entry, ie its path
 *
 * For a given directory, remember the last selected item when the user navigated away from it. Used for easier
 * movement through the file system.
 *
 *  When jumping to distant directory, add paths in-between
 */
class SelectionHistory {

    private val history: MutableMap<Path, Path> = mutableMapOf()

    fun update(forDirectory: Path, selection: Path) {
        println("Update: $forDirectory, $selection")
        history[forDirectory] = selection
    }

    fun getSelected(forDirectory: Path): Path? {
        println("getSelected: $forDirectory")
        return history[forDirectory]
    }

}

// TODO: When remembered selection does not exist in CWD, remove it from history (either got deleted or renamed from dir)
//       - maybe track that? use inode id? overthinking it?
//       - possible ranger bug there?
//       - log for now -test

// TODO: only use name instead of full path?