package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene_tree;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.xdgshell.XdgSurface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.wlr_scene_xdg_surface_create;


/// Node representing a subtree in the scene-graph.
@NullMarked
public class SceneTree {
    public final MemorySegment sceneTreePtr;


    public SceneTree(MemorySegment sceneTreePtr) {
        assert !sceneTreePtr.equals(NULL);
        this.sceneTreePtr = sceneTreePtr;
    }


    public static @Nullable SceneTree ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new SceneTree(ptr) : null;
    }


    public SceneNode node() {
        return new SceneNode(wlr_scene_tree.node(sceneTreePtr));
    }


    /// Add a node displaying a xdg_surface and all of its sub-surfaces to the scene-graph.
    ///
    /// The origin of the returned scene-graph node will match the top-left corner of the xdg_surface window
    /// geometry.
    // TODO: Where should this function go? Scene?
    public SceneTree xdgSurfaceCreate(XdgSurface xdgSurface) {
        return new SceneTree(wlr_scene_xdg_surface_create(sceneTreePtr, xdgSurface.xdgSurfacePtr));
    }
}