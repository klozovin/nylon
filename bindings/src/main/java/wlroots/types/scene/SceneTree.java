package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene_tree;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


/// Node representing a subtree in the scene-graph.
@NullMarked
public class SceneTree {
    public final MemorySegment sceneTreePtr;


    public SceneTree(MemorySegment sceneTreePtr) {
        assert !sceneTreePtr.equals(NULL);
        this.sceneTreePtr = sceneTreePtr;
    }


    public SceneNode node() {
        return new SceneNode(wlr_scene_tree.node(sceneTreePtr));
    }
}