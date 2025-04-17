package xkbcommon;

import jextract.xkbcommon.xkbcommon_h;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static jextract.xkbcommon.xkbcommon_h.xkb_state_key_get_one_sym;
import static jextract.xkbcommon.xkbcommon_h.xkb_state_key_get_syms;


public class XkbState {
    private final @NonNull MemorySegment xkbStatePtr;


    public XkbState(@NonNull MemorySegment xkbStatePtr) {
        this.xkbStatePtr = xkbStatePtr;
    }


    /// Get the single keysym obtained from pressing a particular key in a given keyboard state.
    ///
    /// This function is similar to {@link #keyGetSyms(int)}, but intended for users which cannot or do
    /// not want to handle the case where multiple keysyms are returned (in which case this
    /// function is preferred).
    ///
    /// This function performs Capitalization [keysym transformation](https://xkbcommon.org/doc/current/group__keysyms.html).
    ///
    /// @return The keysym. If the key does not have exactly one keysym, returns {@link XkbKey#NoSymbol}.
    public int keyGetOneSym(int keycode) {
        return xkb_state_key_get_one_sym(xkbStatePtr, keycode);
    }


    /// Get the keysyms obtained from pressing a particular key in a given keyboard state.
    public int[] keyGetSyms(int keycode) {
        try (var arena = Arena.ofConfined()) {
            var keySymsPtr = arena.allocate(xkbcommon_h.C_POINTER);
            var numKeySyms = xkb_state_key_get_syms(xkbStatePtr, keycode, keySymsPtr);
            var keySyms = new int[numKeySyms];
            for (int i = 0; i < numKeySyms; i++) {
                keySyms[i] = keySymsPtr.get(xkbcommon_h.C_POINTER, i).get(xkbcommon_h.xkb_keysym_t, 0);
            }
            return keySyms;
        }
    }
}