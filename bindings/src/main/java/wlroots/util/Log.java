package wlroots.util;

import jextract.wlroots.util.log_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static jextract.wlroots.util.log_h.*;


public final class Log {

    /// ```c
    /// void
    /// wlr_log_init(
    ///     enum wlr_log_importance verbosity,
    ///     wlr_log_func_t callback
    ///)
    ///```
    public static void init(Importance verbosity) {
        wlr_log_init(verbosity.idx, MemorySegment.NULL);
    }


    public static void log(Importance verbosity, String message) {
        try (Arena arena = Arena.ofConfined()) {
            // NOTE: Only specify makeInvoker() arguments when actually using the varargs, here
            //       we just use the first two regular parameters!
            var invoker = log_h._wlr_log.makeInvoker();
            invoker.apply(verbosity.idx, arena.allocateFrom(message));
        }
    }


    public static void logSilent(String message) {
        log(Importance.SILENT, message);
    }


    public static void logError(String message) {
        log(Importance.ERROR, message);
    }


    public static void logInfo(String message) {
        log(Importance.INFO, message);
    }


    public static void logDebug(String message) {
        log(Importance.DEBUG, message);
    }


    public enum Importance {
        SILENT(WLR_SILENT()),
        ERROR(WLR_ERROR()),
        INFO(WLR_INFO()),
        DEBUG(WLR_DEBUG()),
        IMPORTANCE_LAST(WLR_LOG_IMPORTANCE_LAST());

        final int idx;


        Importance(int constant) {
            this.idx = constant;
        }
    }
}