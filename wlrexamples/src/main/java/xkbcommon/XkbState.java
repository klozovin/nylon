package xkbcommon;

import jexxkb.xkbcommon_h;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static jexxkb.xkbcommon_h_1.xkb_state_key_get_one_sym;
import static jexxkb.xkbcommon_h_1.xkb_state_key_get_syms;


public class XkbState {
    private final @NonNull MemorySegment xkbStatePtr;


    public XkbState(@NonNull MemorySegment xkbStatePtr) {
        this.xkbStatePtr = xkbStatePtr;
    }


    /// Get the single keysym obtained from pressing a particular key in a given keyboard state.
    ///
    /// @return The keysym. If the key does not have exactly one keysym, returns XKB_KEY_NoSymbol
    public int getOneSym(int keycode) {
        return xkb_state_key_get_one_sym(xkbStatePtr, keycode);
    }


    public int[] getSyms(int keycode) {
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