package wlroots.types.xdgshell;

import org.jspecify.annotations.NullMarked;
import wayland.server.Display;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xdg_shell_h.wlr_xdg_shell_create;


/// `struct wlr_xdg_shell {};`
@NullMarked
public class XdgShell {
    MemorySegment xdgShellPtr;


    public XdgShell(MemorySegment xdgShellPtr) {
        assert !xdgShellPtr.equals(NULL);
        this.xdgShellPtr = xdgShellPtr;
    }


    /// Create the xdg_wm_base global with the specified version.
    public static XdgShell create(Display display, int version) {
        return new XdgShell(wlr_xdg_shell_create(display.displayPtr, version));
    }
}