package wlroots.types.scene;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_node_set_position;


@NullMarked
public class SceneNode {
    public final MemorySegment sceneNodePtr;


    public SceneNode(MemorySegment sceneNodePtr) {
        assert !sceneNodePtr.equals(NULL);
        this.sceneNodePtr = sceneNodePtr;
    }


    public void setPosition(int x, int y) {
        wlr_scene_node_set_position(sceneNodePtr, x, y);
    }
}