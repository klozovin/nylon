package wlroots.types.scene;

import jextract.wlroots.wlr_scene_tree;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import wlroots.types.xdg_shell.XdgSurface;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.wlr.wlr_scene_tree_from_node;
import static jextract.wlroots.wlr.wlr_scene_xdg_surface_create;


/// Node representing a subtree in the scene-graph.
@NullMarked
public final class SceneTree {
    public final MemorySegment sceneTreePtr;


    public SceneTree(MemorySegment sceneTreePtr) {
        assert !sceneTreePtr.equals(NULL);
        this.sceneTreePtr = sceneTreePtr;
    }


    @Override
    public boolean equals(Object other) {
        return switch (other) {
            case SceneTree otherSceneTree -> sceneTreePtr.equals(otherSceneTree.sceneTreePtr);
            case null -> false;
            default -> throw new RuntimeException("BUG: Trying to compare objects of different types");
        };
    }


    @Override
    public int hashCode() {
        return sceneTreePtr.hashCode();
    }


    public static @Nullable SceneTree ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new SceneTree(ptr) : null;
    }


    /// Convenience function, does not exist in wlroots. Create a new SceneTree for `xdgSurface`, but with
    /// parented in `parent.`
    public static SceneTree createFromParent(SceneTree parent, XdgSurface xdgSurface) {
        return parent.xdgSurfaceCreate(xdgSurface);
    }


    /// If this node represents a wlr_scene_tree, that tree will be returned. It is not legal to feed a node
    /// that does not represent a wlr_scene_tree.
    public static SceneTree fromNode(SceneNode node) {
        return new SceneTree(wlr_scene_tree_from_node(node.sceneNodePtr));
    }


    // *** Getters and setters **************************************************************************** //


    public SceneNode getNode() {
        return new SceneNode(wlr_scene_tree.node(sceneTreePtr));
    }


    // *** Methods **************************************************************************************** //


    /// Add a node displaying a xdg_surface and all of its sub-surfaces to the scene-graph, with this SceneTree
    /// as its parent.
    ///
    /// The origin of the returned scene-graph node will match the top-left corner of the xdg_surface window
    /// geometry.
    ///
    /// @return SceneTree that was created and attached to the `xdgSurface` that was passed in
    ///
    public SceneTree xdgSurfaceCreate(XdgSurface xdgSurface) {
        return new SceneTree(wlr_scene_xdg_surface_create(sceneTreePtr, xdgSurface.xdgSurfacePtr));
    }
}