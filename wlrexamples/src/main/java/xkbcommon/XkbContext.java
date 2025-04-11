package xkbcommon;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jexxkbcommon.xkbcommon_h.*;


public class XkbContext {
    public final @NonNull MemorySegment xkbContextPtr;


    public XkbContext(@NonNull MemorySegment xkbContextPtr) {
        this.xkbContextPtr = xkbContextPtr;
    }


    /// Create a new context.
    public static @Nullable XkbContext newContext(@NonNull Flags flags) {
        // TODO: can return null
        var xkbContextPtr = xkb_context_new(flags.constant);
        if (xkbContextPtr != NULL)
            return new XkbContext(xkbContextPtr);
        else
            return null;
    }


    public void keymapNewFromNames() {
        // TODO
    }


    public enum Flags {
        NO_FLAGS(XKB_CONTEXT_NO_FLAGS()),
        NO_DEFAULT_INCLUDES(XKB_CONTEXT_NO_DEFAULT_INCLUDES()),
        NO_ENVIRONMENT_NAMES(XKB_CONTEXT_NO_ENVIRONMENT_NAMES()),
        NO_SECURE_GETENV(XKB_CONTEXT_NO_SECURE_GETENV());

        private final int constant;


        Flags(int constant) {
            this.constant = constant;
        }
    }
}