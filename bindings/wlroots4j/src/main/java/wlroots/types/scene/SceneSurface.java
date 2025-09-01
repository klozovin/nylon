package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene_surface;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.compositor.Surface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_surface_create;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_surface_try_from_buffer;


/// A scene-graph node displaying a single surface.
@NullMarked
public class SceneSurface {
    public final MemorySegment sceneSurfacePtr;


    public SceneSurface(MemorySegment sceneSurfacePtr) {
        assert !sceneSurfacePtr.equals(NULL);
        this.sceneSurfacePtr = sceneSurfacePtr;
    }


    public static @Nullable SceneSurface ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new SceneSurface(ptr) : null;
    }


    /// Add a node displaying a single surface to the scene-graph.
    public static SceneSurface create(SceneTree parent, Surface surface) {
        return new SceneSurface(wlr_scene_surface_create(parent.sceneTreePtr, surface.surfacePtr));
    }


    /// If this buffer is backed by a surface, then the struct wlr_scene_surface is returned. If not, NULL
    /// will be returned.
    public static @Nullable SceneSurface tryFromBuffer(SceneBuffer buffer) {
        return SceneSurface.ofPtrOrNull(wlr_scene_surface_try_from_buffer(buffer.sceneBufferPtr));

    }


    // *** Methods **************************************************************************************** //


    public SceneBuffer buffer() {
        return new SceneBuffer(wlr_scene_surface.buffer(sceneSurfacePtr));
    }


    public Surface surface() {
        return new Surface(wlr_scene_surface.surface(sceneSurfacePtr));
    }
}