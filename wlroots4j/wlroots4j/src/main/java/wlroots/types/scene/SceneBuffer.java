package wlroots.types.scene;

import jextract.wlroots.wlr_scene_buffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.buffer.Buffer;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.wlr.wlr_scene_buffer_create;
import static jextract.wlroots.wlr.wlr_scene_buffer_from_node;


/// A scene-graph node displaying a buffer
///
/// `struct wlr_scene_buffer {};`
@NullMarked
public final class SceneBuffer extends SceneNode {
    MemorySegment sceneBufferPtr;


    public SceneBuffer(MemorySegment sceneBufferPtr) {
        assert !sceneBufferPtr.equals(NULL);
        super(wlr_scene_buffer.node(sceneBufferPtr));
        this.sceneBufferPtr = sceneBufferPtr;
    }


    @Override
    public boolean equals(@Nullable Object other) {
        return switch (other) {
            case null -> false;
            case SceneBuffer otherSceneBuffer -> sceneBufferPtr.equals(otherSceneBuffer.sceneBufferPtr);
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    public static SceneBuffer create(SceneTree parent, Buffer buffer) {
        return new SceneBuffer(wlr_scene_buffer_create(parent.sceneTreePtr, buffer.bufferPtr));
    }


    /// If this node represents a wlr_scene_buffer, that buffer will be returned. It is not legal to feed a
    /// node that does not represent a wlr_scene_buffer.
    public static SceneBuffer fromNode(SceneNode node) {
        return new SceneBuffer(wlr_scene_buffer_from_node(node.sceneNodePtr));
    }
}