package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene_buffer;
import org.jspecify.annotations.NullMarked;
import wlroots.types.buffer.Buffer;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.*;


///  A scene-graph node displaying a buffer
///
/// `struct wlr_scene_buffer {};`
@NullMarked
public final class SceneBuffer {
    MemorySegment sceneBufferPtr;


    public SceneBuffer(MemorySegment sceneBufferPtr) {
        assert !sceneBufferPtr.equals(NULL);
        this.sceneBufferPtr = sceneBufferPtr;
    }


    public static SceneBuffer create(SceneTree parent, Buffer buffer) {
        return new SceneBuffer(wlr_scene_buffer_create(parent.sceneTreePtr, buffer.bufferPtr));
    }


    /// If this node represents a wlr_scene_buffer, that buffer will be returned. It is not legal to feed a
    /// node that does not represent a wlr_scene_buffer.
    public static SceneBuffer fromNode(SceneNode node) {
        return new SceneBuffer(wlr_scene_buffer_from_node(node.sceneNodePtr));
    }


    // *** Getters and setters **************************************************************************** //


    public SceneNode node() {
        return new SceneNode(wlr_scene_buffer.node(sceneBufferPtr));
    }
}