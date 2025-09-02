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


    /// Custom copy constructor that allocates a Box in a GC collected arena.
    public static Box allocateCopy(Box box) {
        var newBox = Box.allocate(Arena.ofAuto());
        newBox.setX(box.getX());
        newBox.setY(box.getY());
        newBox.setWidth(box.getWidth());
        newBox.setHeight(box.getHeight());
        return newBox;
    }


    @Override
    public String toString() {
        return "Box[" +
            "x=" + getX() +
            ", y=" + getY() +
            ", width=" + getWidth() +
            ", height=" + getHeight() +
            "]";
    }

    // *** Getters and setters **************************************************************************** //


    public int getX() {
        return wlr_box.x(boxPtr);
    }


    public void setX(int x) {
        wlr_box.x(boxPtr, x);
    }


    public int getY() {
        return wlr_box.y(boxPtr);
    }


    public void setY(int y) {
        wlr_box.y(boxPtr, y);
    }


    public int getWidth() {
        return wlr_box.width(boxPtr);
    }


    public void setWidth(int width) {
        wlr_box.width(boxPtr, width);
    }


    public int getHeight() {
        return wlr_box.height(boxPtr);
    }


    public void setHeight(int height) {
        wlr_box.height(boxPtr, height);
    }
}