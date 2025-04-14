package wlroots.wlr.util;

import jextract.wlroots.wlr_box;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;


/// A box representing a rectangle region in a 2D space.
///
/// The x and y coordinates are inclusive, and the width and height lengths are exclusive. In other
///  words, the box starts from the coordinates (x, y), and goes up to but not including
/// (x + width, y + height).
public class Box {
    public final @NonNull MemorySegment boxPtr;


    public Box(@NotNull MemorySegment boxPtr) {
        this.boxPtr = boxPtr;
    }


    public void setWidth(int width) {
        wlr_box.width(boxPtr, width);
    }


    public void setHeight(int height) {
        wlr_box.height(boxPtr, height);
    }
}