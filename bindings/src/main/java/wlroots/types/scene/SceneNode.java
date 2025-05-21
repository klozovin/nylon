package wlroots.types.scene;

import jextract.wlroots.types.wlr_scene_node;
import nylon.Tuple;
import nylon.Tuple.Tuple3;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Iterator;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.types.wlr_scene_h.*;


@NullMarked
public class SceneNode {
    public final MemorySegment sceneNodePtr;

    /// Iterate over the .parent field all the way up to the root of the tree.
    public final Iterable<SceneTree> parentIterator = ParentIterator::new;


    public SceneNode(MemorySegment sceneNodePtr) {
        assert !sceneNodePtr.equals(NULL);
        this.sceneNodePtr = sceneNodePtr;
    }


    // *** Getters and setters **************************************************************************** //


    public Type type() {
        return Type.of(wlr_scene_node.type(sceneNodePtr));
    }


    /// Can be NULL when accessing wlr_scene.tree.node.parent.
    public @Nullable SceneTree parent() {
        return SceneTree.ofPtrOrNull(wlr_scene_node.parent(sceneNodePtr));
    }


    /// Relative to parent.
    public int x() {
        return wlr_scene_node.x(sceneNodePtr);
    }


    /// Relative to parent.
    public int y() {
        return wlr_scene_node.y(sceneNodePtr);
    }

    // *** Methods **************************************************************************************** //


    /// Set the position of the node relative to its parent.
    public void setPosition(int x, int y) {
        wlr_scene_node_set_position(sceneNodePtr, x, y);
    }


    /// Move the node above all of its sibling nodes.
    public void raiseToTop() {
        wlr_scene_node_raise_to_top(sceneNodePtr);
    }


    /// Find the topmost node in this scene-graph that contains the target point at the given layout-local
    /// coordinates. (For surface nodes, this means accepting input events at that point.) Returns the node
    /// and coordinates relative to the returned node, or NULL if no node is found at that location.
    ///
    /// @param lx Target point, layout-local x coordinate
    /// @param ly Target point, layout-local y coordinate
    /// @return Found {@link SceneNode} coordinates relative to the returned node, or NULL if no node found
    public @Nullable Tuple3<SceneNode, Double, Double> nodeAt(double lx, double ly) {
        try (var arena = Arena.ofConfined()) {
            var nxPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
            var nyPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
            var nodePtr = wlr_scene_node_at(sceneNodePtr, lx, ly, nxPtr, nyPtr);

            return !nodePtr.equals(NULL) ?
                Tuple.of(
                    new SceneNode(nodePtr),
                    nxPtr.get(ValueLayout.JAVA_DOUBLE, 0),
                    nyPtr.get(ValueLayout.JAVA_DOUBLE, 0))
                : null;
        }
    }


    /// Immediately destroy the scene-graph node.
    public void destroy() {
        wlr_scene_node_destroy(sceneNodePtr);
    }


    public class ParentIterator implements Iterator<SceneTree> {
        private SceneNode node;


        public ParentIterator() {
            this.node = SceneNode.this;
        }


        @Override
        public boolean hasNext() {
            return this.node.parent() != null;
        }


        @Override
        public SceneTree next() {
            var next = node.parent();
            assert next != null;
            node = next.node();
            return next;
        }
    }


    public enum Type {
        TREE(WLR_SCENE_NODE_TREE()),
        RECT(WLR_SCENE_NODE_RECT()),
        BUFFER(WLR_SCENE_NODE_BUFFER());

        public final int value;


        Type(int value) {
            this.value = value;
        }


        public static Type of(int value) {
            for (var e : values()) {
                if (e.value == value)
                    return e;
            }
            throw new RuntimeException("Invalid enum value from C code for wlr_scene_node_type");
        }

    }
}