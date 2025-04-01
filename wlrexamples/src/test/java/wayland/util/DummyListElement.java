package wayland.util;

import jexwayland.wl_list;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;


public class DummyListElement {
    public final MemorySegment elementPTr;

    public static MemoryLayout LAYOUT = MemoryLayout.structLayout(
        JAVA_INT.withName("x"),
        MemoryLayout.paddingLayout(4),

        wl_list.layout().withName("link"),

        JAVA_INT.withName("y"),
        MemoryLayout.paddingLayout(4),

        JAVA_LONG.withName("z")
    );


    public void setX(int x) {
        LAYOUT.varHandle(groupElement("x")).set(
            elementPTr,
            LAYOUT.byteOffset(groupElement("x")),
            x
        );
    }


    public int getX() {
        return (int) LAYOUT.varHandle(groupElement("x")).get(
            elementPTr,
            LAYOUT.byteOffset(groupElement("x"))
        );
    }


    public void setZ(int z) {
        LAYOUT.varHandle(groupElement("z")).set(
            elementPTr,
            0,
            z
        );
    }


    public long getZ() {
        return (long) LAYOUT.varHandle(groupElement("z")).get(
            elementPTr,
            0
        );
    }

    DummyListElement(MemorySegment elementPTr) {
        this.elementPTr = elementPTr;
    }


    DummyListElement(Arena arena) {
        this(arena.allocate(LAYOUT));
    }

}
