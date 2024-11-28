package wlroots;
import wlroots.util.log_h;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public final class Log {

    public enum Importance {
        SILENT,
        ERROR,
        INFO,
        DEBUG,
        IMPORTANCE_LAST;

        int getConstant() {
            return switch (this) {
                case SILENT -> log_h.WLR_SILENT();
                case ERROR -> log_h.WLR_ERROR();
                case INFO -> log_h.WLR_INFO();
                case DEBUG -> log_h.WLR_DEBUG();
                case IMPORTANCE_LAST -> log_h.WLR_LOG_IMPORTANCE_LAST();
            };
        }
    }

    /**
     * {@snippet lang=c :
     * void wlr_log_init(enum wlr_log_importance verbosity, wlr_log_func_t callback)
     * }
     */
    public static void init(Importance verbosity) {
        log_h.wlr_log_init(verbosity.getConstant(), MemorySegment.NULL);
    }

    public static void log(Importance verbosity, String message) {
        try(Arena arena = Arena.ofConfined()) {
            var invoker = log_h._wlr_log.makeInvoker();
            invoker.apply(verbosity.getConstant(), arena.allocateFrom(message));
        }
    }
}
