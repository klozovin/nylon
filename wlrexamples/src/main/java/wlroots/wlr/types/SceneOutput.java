package wlroots.wlr.types;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_output_commit;


@NullMarked
public class SceneOutput {
    public final MemorySegment sceneOutputPtr;


    public SceneOutput(MemorySegment sceneOutputPtr) {
        assert !sceneOutputPtr.equals(NULL);
        this.sceneOutputPtr = sceneOutputPtr;
    }


    public boolean commit() {
        return wlr_scene_output_commit(sceneOutputPtr, NULL);
    }
}