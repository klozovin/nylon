package wlroots.wlr.types;

import jextract.wlroots.types.wlr_scene_surface;
import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_surface_create;


@NullMarked
public class SceneSurface {
    public final MemorySegment sceneSurfacePtr;


    public SceneSurface(MemorySegment sceneSurfacePtr) {
        assert !sceneSurfacePtr.equals(NULL);
        this.sceneSurfacePtr = sceneSurfacePtr;
    }


    /// Add a node displaying a single surface to the scene-graph.
    public static SceneSurface create(SceneTree parent, Surface surface) {
        return new SceneSurface(wlr_scene_surface_create(parent.sceneTreePtr, surface.surfacePtr));
    }


    public SceneBuffer buffer() {
        return new SceneBuffer(wlr_scene_surface.buffer(sceneSurfacePtr));
    }
}