package wlroots.types;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_xcursor_manager_h.wlr_xcursor_manager_create;
import static jextract.wlroots.types.wlr_xcursor_manager_h.wlr_xcursor_manager_destroy;


/// Loads Xcursor themes to source cursor images, makes sure that cursor images are available at all scale
/// factors on the screen (for HiDPI).
@NullMarked
public class XcursorManager {
    public final MemorySegment xcursorManagerPtr;


    public XcursorManager(MemorySegment xcursorManagerPtr) {
        assert !xcursorManagerPtr.equals(NULL);
        this.xcursorManagerPtr = xcursorManagerPtr;
    }


    /// Creates a new XCursor manager with the given xcursor theme name and base size (for use when scale=1).
    public static @Nullable XcursorManager create(@Nullable String name, int size) {
        try (var arena = Arena.ofConfined()) {
            var xcursorManagerPtr = wlr_xcursor_manager_create(switch (name) {
                case String n -> arena.allocateFrom(n);
                case null -> NULL;
            }, size);

            // wlr_xcursor_manager_create can return NULL if calloc() inside of it returns NULL.
            return !xcursorManagerPtr.equals(NULL) ? new XcursorManager(xcursorManagerPtr) : null;
        }
    }


    public void destroy() {
        wlr_xcursor_manager_destroy(xcursorManagerPtr);
    }
}