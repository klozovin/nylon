package wlroots.util;

import jextract.wlroots.wlr;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static jextract.wlroots.wlr.*;


public final class Log {

    /// ```c
    /// void
    /// wlr_log_init(
    ///     enum wlr_log_importance verbosity,
    ///     wlr_log_func_t callback
    /// )
    /// ```
    public static void init(Importance verbosity) {
        wlr_log_init(verbosity.idx, MemorySegment.NULL);
    }


    public static void log(Importance verbosity, String message) {
        try (Arena arena = Arena.ofConfined()) {
            // NOTE: Only specify makeInvoker() arguments when actually using the varargs, here
            //       we just use the first two regular parameters!
            var invoker = wlr._wlr_log.makeInvoker();
            invoker.apply(verbosity.idx, arena.allocateFrom(message));
        }
    }


    public static void logSilent(String message) {
        log(Importance.Silent, message);
    }


    public static void logError(String message) {
        log(Importance.Error, message);
    }


    public static void logInfo(String message) {
        log(Importance.Info, message);
    }


    public static void logDebug(String message) {
        log(Importance.Debug, message);
    }


    public enum Importance {
        Silent(WLR_SILENT()),
        Error(WLR_ERROR()),
        Info(WLR_INFO()),
        Debug(WLR_DEBUG()),
        IMPORTANCE_LAST(WLR_LOG_IMPORTANCE_LAST());

        final int idx;


        Importance(int constant) {
            this.idx = constant;
        }
    }
}