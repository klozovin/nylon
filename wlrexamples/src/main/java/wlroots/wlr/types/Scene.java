package wlroots.wlr.types;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_create;


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
}