package tinywl

import tinywl.wlroots.util.log_h
import tinywl.wlroots.version_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment


class TinyWL {
    fun main() {
        println("TinyWL running on Java: ${System.getProperty("java.version")}")
        println("Wlroots (major/minor/micro): " +
                "${version_h.WLR_VERSION_MAJOR()}." +
                "${version_h.WLR_VERSION_MINOR()}." +
                "${version_h.WLR_VERSION_MICRO()}")
        println("Wlroots (str): ${version_h.WLR_VERSION_STR().getUtf8String(0)}")

        // Let's try logging a string
        log_h.wlr_log_init(log_h.WLR_DEBUG(), log_h.NULL())
        println("Log verbosity should be: ${log_h.WLR_DEBUG()}")
        println("Log verbosity is: ${log_h.wlr_log_get_verbosity()}")
        Arena.ofConfined().use {
            val log_message = "Hello, world!"
            val segment = it.allocate((log_message.length+1).toLong())
            segment.setUtf8String(0, log_message)
            log_h._wlr_log(log_h.WLR_INFO(), segment)
        }
    }
}
