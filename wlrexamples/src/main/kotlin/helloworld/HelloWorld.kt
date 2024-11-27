package helloworld

import wlroots.util.log_h.*
import wlroots.version_h.*
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment


/**
 * {@snippet lang=c :
 * void _wlr_log(enum wlr_log_importance verbosity, const char *format, ...)
 *
 */
fun wlr_log(verbosity: Int, message: String) {
    val invoker = _wlr_log.makeInvoker()
    Arena.ofConfined().use { arena ->
        invoker.apply(verbosity, arena.allocateFrom(message))
    }
}

fun main() {
    println("Java: ${System.getProperty("java.version")}")

    wlr_log_init(WLR_DEBUG(), MemorySegment.NULL)
    wlr_log(WLR_INFO(), "Hello world")
    wlr_log(WLR_INFO(), "Running on wlroots version: ${WLR_VERSION_STR().getString(0)}")
}