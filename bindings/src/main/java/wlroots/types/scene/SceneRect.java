package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene_rect;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.*;


/// A scene-graph node displaying a solid-colored rectangle.
///
/// `struct wlr_scene_rect {};`
@NullMarked
public final class SceneRect {
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


    /// If this node represents a wlr_scene_rect, that rect will be returned. It is not legal to feed a node
    /// that does not represent a wlr_scene_rect.
    public static SceneRect fromNode(SceneNode node) {
        return new SceneRect(wlr_scene_rect_from_node(node.sceneNodePtr));
    }


    // *** Getters and setters **************************************************************************** //


    public SceneNode node() {
        return new SceneNode(wlr_scene_rect.node(sceneRectPtr));
    }


    // *** Methods **************************************************************************************** //


    /// Change the width and height of an existing rectangle node.
    public void setSize(int width, int height) {
        wlr_scene_rect_set_size(sceneRectPtr, width, height);
    }
}