package wlroots.types.scene;

import jextract.wlroots.timespec;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.*;


/// A viewport for an output in the scene-graph.
@NullMarked
public class SceneOutput {
    public final MemorySegment sceneOutputPtr;


    public SceneOutput(MemorySegment sceneOutputPtr) {
        assert !sceneOutputPtr.equals(NULL);
        this.sceneOutputPtr = sceneOutputPtr;
    }


    /// Call wlr_surface_send_frame_done() on all surfaces in the scene rendered by
    /// {@link #commit()} for which wlr_scene_surface.primary_output matches the given scene_output.
    public void sendFrameDone() {
        // TODO: Implement the overload with time parameter (Instant, getEpochSeconds(), getNano)
        try (var arena = Arena.ofConfined()) {
            var timeSpecPtr = timespec.allocate(arena);
            clock_gettime(CLOCK_MONOTONIC(), timeSpecPtr);
            wlr_scene_output_send_frame_done(sceneOutputPtr, timeSpecPtr);
        }
    }


    public boolean commit() {
        // TODO: Implement commit() overload when options are not null
        return wlr_scene_output_commit(sceneOutputPtr, NULL);
    }
}