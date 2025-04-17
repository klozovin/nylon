package wlroots.wlr.types;

import jextract.wlroots.types.wlr_scene_buffer;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_buffer_create;


///  A scene-graph node displaying a buffer
@NullMarked
public class SceneBuffer {
    MemorySegment sceneBufferPtr;


    public SceneBuffer(MemorySegment sceneBufferPtr) {
        assert !sceneBufferPtr.equals(NULL);
        this.sceneBufferPtr = sceneBufferPtr;
    }


    public static SceneBuffer create(SceneTree parent, Buffer buffer) {
        return new SceneBuffer(wlr_scene_buffer_create(parent.sceneTreePtr, buffer.bufferPtr));
    }

    /*** Struct getters ***/

    public SceneNode node() {
        return new SceneNode(wlr_scene_buffer.node(sceneBufferPtr));
    }
}