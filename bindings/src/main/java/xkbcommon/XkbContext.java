package xkbcommon;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.xkbcommon.xkbcommon_h.*;


public class XkbContext {
    public final @NonNull MemorySegment xkbContextPtr;


    public XkbContext(@NonNull MemorySegment xkbContextPtr) {
        assert !xkbContextPtr.equals(NULL);
        this.xkbContextPtr = xkbContextPtr;
    }


    /// Create a new context.
    ///
    /// @param flags Optional flags for the context, or 0.
    /// @return A new context, or NULL on failure.
    public static @Nullable XkbContext of(@NonNull Flags flags) {
        var xkbContextPtr = xkb_context_new(flags.idx);
        return !xkbContextPtr.equals(NULL) ? new XkbContext(xkbContextPtr) : null;
    }


    public @Nullable Keymap keymapNewFromNames(@Nullable RuleNames names, Keymap.CompileFlags flags) {
        var keymapPtr = xkb_keymap_new_from_names(
            xkbContextPtr,
            switch (names) {
                case RuleNames n -> n.ruleNamesPtr;
                case null -> NULL;
            },
            flags.idx
        );
        return !keymapPtr.equals(NULL) ? new Keymap(keymapPtr) : null;
    }


    ///  Release a reference on a context, and possibly free it.
    public void unref() {
        xkb_context_unref(xkbContextPtr);
    }


    public enum Flags {
        NO_FLAGS(XKB_CONTEXT_NO_FLAGS()),
        NO_DEFAULT_INCLUDES(XKB_CONTEXT_NO_DEFAULT_INCLUDES()),
        NO_ENVIRONMENT_NAMES(XKB_CONTEXT_NO_ENVIRONMENT_NAMES()),
        NO_SECURE_GETENV(XKB_CONTEXT_NO_SECURE_GETENV());

        public final int idx;


        Flags(int constant) {
            this.idx = constant;
        }
    }
}