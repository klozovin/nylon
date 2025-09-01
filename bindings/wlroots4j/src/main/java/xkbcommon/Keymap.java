package xkbcommon;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.xkbcommon.xkbcommon_h.XKB_KEYMAP_COMPILE_NO_FLAGS;
import static jextract.xkbcommon.xkbcommon_h.xkb_keymap_unref;


@NullMarked
public class Keymap {
    public final MemorySegment keymapPtr;


    public Keymap(MemorySegment keymapPtr) {
        assert !keymapPtr.equals(NULL);
        this.keymapPtr = keymapPtr;
    }


    /// Release a reference on a keymap, and possibly free it.
    public void unref() {
        xkb_keymap_unref(keymapPtr);
    }


    public enum CompileFlags {
        NO_FLAGS(XKB_KEYMAP_COMPILE_NO_FLAGS());

        public final int idx;


        CompileFlags(int constant) {
            this.idx = constant;
        }
    }
}