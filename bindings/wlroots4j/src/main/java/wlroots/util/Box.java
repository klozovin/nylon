package wlroots.util;

import jextract.wlroots.wlr_box;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;


/// A box representing a rectangle region in a 2D space.
///
/// The x and y coordinates are inclusive, and the width and height lengths are exclusive. In other words, the
/// box starts from the coordinates (x, y), and goes up to but not including (x + width, y + height).
@NullMarked
public class Box {
    public final MemorySegment boxPtr;


    public Box(MemorySegment boxPtr) {
        this.boxPtr = boxPtr;
    }


    public static Box allocate(Arena arena) {
        return new Box(wlr_box.allocate(arena));
    }


    @Override
    public String toString() {
        return "Box[" +
            "x=" + x() +
            ", y=" + y() +
            ", width=" + width() +
            ", height=" + height() +
            "]";
    }

    // *** Getters and setters **************************************************************************** //


    public int x() {
        return wlr_box.x(boxPtr);
    }


    public void x(int x) {
        wlr_box.x(boxPtr, x);
    }


    public int y() {
        return wlr_box.y(boxPtr);
    }


    public void y(int y) {
        wlr_box.y(boxPtr, y);
    }


    public int width() {
        return wlr_box.width(boxPtr);
    }


    public void setWidth(int width) {
        wlr_box.width(boxPtr, width);
    }


    public int height() {
        return wlr_box.height(boxPtr);
    }


    public void setHeight(int height) {
        wlr_box.height(boxPtr, height);
    }
}