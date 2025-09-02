package wlroots.types.xdgshell;

import jextract.wlroots.types.wlr_xdg_shell;
import org.jspecify.annotations.NullMarked;
import wayland.server.Display;
import wayland.server.Signal;
import wayland.server.Signal.Signal1;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xdg_shell_h.wlr_xdg_shell_create;


/// `struct wlr_xdg_shell {}`
@NullMarked
public class XdgShell {
    private final MemorySegment xdgShellPtr;
    public final Events events;



    public XdgShell(MemorySegment xdgShellPtr) {
        assert !xdgShellPtr.equals(NULL);
        this.xdgShellPtr = xdgShellPtr;
        this.events = new Events(wlr_xdg_shell.events(xdgShellPtr));
    }


    /// Create the xdg_wm_base global with the specified version.
    public static XdgShell create(Display display, int version) {
        return new XdgShell(wlr_xdg_shell_create(display.displayPtr, version));
    }


    public static class Events {
        private final MemorySegment eventsPtr;

        ///  Raised when a new toplevel window is created
        public final Signal1<XdgToplevel> newToplevel;

        /// Raised when a client creates a new popup.
        public final Signal1<XdgPopup> newPopup;

        ///  Raised when destroying the XdgShell
        public final Signal1<XdgShell> destroy;


        public Events(MemorySegment eventsPtr) {
            this.eventsPtr   = eventsPtr;
            this.newToplevel = Signal.of(wlr_xdg_shell.events.new_toplevel(eventsPtr), XdgToplevel::new);
            this.newPopup    = Signal.of(wlr_xdg_shell.events.new_popup(eventsPtr),    XdgPopup::new);
            this.destroy     = Signal.of(wlr_xdg_shell.events.destroy(eventsPtr),    XdgShell::new);
        }
    }
}