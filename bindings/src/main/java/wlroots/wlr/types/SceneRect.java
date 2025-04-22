package wlroots.wlr.types;

import jextract.wlroots.types.wlr_scene_rect;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_rect_create;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_rect_set_size;


@NullMarked
public class SceneRect {
    public final MemorySegment sceneRectPtr;


    public SceneRect(MemorySegment sceneRectPtr) {
        assert !sceneRectPtr.equals(NULL);
        this.sceneRectPtr = sceneRectPtr;
    }


    public static SceneRect create(SceneTree parent, int width, int height, float[] color) {
        try (var arena = Arena.ofConfined()) {
            return new SceneRect(wlr_scene_rect_create(
                parent.sceneTreePtr,
                width, height,
                arena.allocateFrom(ValueLayout.JAVA_FLOAT, color)));
        }
    }


    /// Change the width and height of an existing rectangle node.
    public void setSize(int width, int height) {
        wlr_scene_rect_set_size(sceneRectPtr, width, height);
    }


    public SceneNode node() {
        return new SceneNode(wlr_scene_rect.node(sceneRectPtr));
    }
}