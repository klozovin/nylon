package wlroots;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static jexwlroots.util.log_h.*;
import static jexwlroots.util.log_h._wlr_log.makeInvoker;


public final class Log {

    public enum Importance {
        SILENT,
        ERROR,
        INFO,
        DEBUG,
        IMPORTANCE_LAST;

        int getConstant() {
            return switch (this) {
                case SILENT -> WLR_SILENT();
                case ERROR -> WLR_ERROR();
                case INFO -> WLR_INFO();
                case DEBUG -> WLR_DEBUG();
                case IMPORTANCE_LAST -> WLR_LOG_IMPORTANCE_LAST();
            };
        }

    }

    /**
     * {@snippet lang = c:
     * void wlr_log_init(enum wlr_log_importance verbosity, wlr_log_func_t callback)
     *}
     */
    public static void init(Importance verbosity) {
        wlr_log_init(verbosity.getConstant(), MemorySegment.NULL);
    }

    public static void log(Importance verbosity, String message) {
        try (Arena arena = Arena.ofConfined()) {
            var invoker = makeInvoker();
            invoker.apply(verbosity.getConstant(), arena.allocateFrom(message));
        }
    }

    public static void logInfo(String message) {
        log(Importance.INFO, message);
    }

    public static void logError(String message) {
        log(Importance.ERROR, message);
    }

    public static void logDebug(String message) {
        log(Importance.DEBUG, message);
    }
}
