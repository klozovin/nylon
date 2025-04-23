package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene;
import org.jspecify.annotations.NullMarked;
import wlroots.types.output.Output;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_create;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_output_create;


/// The root scene-graph node.
///
/// `struct wlr_scene {};`
@NullMarked
public class Scene {
    MemorySegment scenePtr;


    private Scene(MemorySegment scenePtr) {
        assert !scenePtr.equals(NULL);
        this.scenePtr = scenePtr;
    }


    /// Create a new scene-graph.
    static public Scene create() {
        return new Scene(wlr_scene_create());
    }


    /*** Struct getters ***/

    public SceneTree tree() {
        return new SceneTree(wlr_scene.tree(scenePtr));
    }


    /*** Methods ***/

    /// Add a viewport for the specified output to the scene-graph.
    ///
    /// An output can only be added once to the scene-graph.
    public SceneOutput outputCreate(Output output) {
        return new SceneOutput(wlr_scene_output_create(scenePtr, output.outputPtr));
    }
}