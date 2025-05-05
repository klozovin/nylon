package wlroots.types.scene;

import jextract.wlroots.timespec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.output.Output;

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


    public static @Nullable SceneOutput ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new SceneOutput(ptr) : null;
    }


    /// Add a viewport for the specified output to the scene-graph.
    ///
    /// An output can only be added once to the scene-graph.
    public static SceneOutput create(Scene scene, Output output) {
        return new SceneOutput(wlr_scene_output_create(scene.scenePtr, output.outputPtr));
    }


    /// Call wlr_surface_send_frame_done() on all surfaces in the scene rendered by {@link #commit()} for
    /// which wlr_scene_surface.primary_output matches the given scene_output.
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