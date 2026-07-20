package wlroots.types.scene;

import jextract.wlroots.wlr_scene_node;
import nylon.Coordinates;
import nylon.Tuple;
import nylon.Tuple.Tuple3;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Iterator;

import static java.lang.foreign.MemorySegment.NULL;
import static jextract.wlroots.wlr.*;


@NullMarked
public sealed class SceneNode permits SceneTree, SceneBuffer, SceneRect {
    public final MemorySegment sceneNodePtr;

    /// Iterate over the .parent field all the way up to the root of the tree.
    public final Iterable<SceneTree> parentIterator = ParentIterator::new;


    public SceneNode(MemorySegment sceneNodePtr) {
        assert !sceneNodePtr.equals(NULL);
        this.sceneNodePtr = sceneNodePtr;
    }


    //
    // *** Fields ***
    //

    public Type getType() {
        return Type.of(wlr_scene_node.type(sceneNodePtr));
    }


    /// Can be NULL when accessing wlr_scene.tree.node.parent.
    public @Nullable SceneTree getParent() {
        return SceneTree.ofPtrOrNull(wlr_scene_node.parent(sceneNodePtr));
    }


    /// Relative to parent.
    public int getX() {
        return wlr_scene_node.x(sceneNodePtr);
    }


    /// Relative to parent.
    public int getY() {
        return wlr_scene_node.y(sceneNodePtr);
    }


    //
    // *** Methods ***
    //

    /// Enable or disable this node. If a node is disabled, all of its children are implicitly disabled as
    /// well.
    public void setEnabled(boolean enabled) {
        wlr_scene_node_set_enabled(sceneNodePtr, enabled);
    }


    /// Set the position of the node relative to its parent.
    public void setPosition(int x, int y) {
        wlr_scene_node_set_position(sceneNodePtr, x, y);
    }


    /// Set the position of the node relative to its parent.
    public void setPosition(double x, double y) {
        wlr_scene_node_set_position(sceneNodePtr, (int) x, (int) y);
    }


    /// @return Node's layout-local coordinates. True is returned if the node and all of its ancestors are
    ///     enabled.
    public Coordinates coords() {
        // TODO: Implement boolean return also! Damnit Java and multiple return values
        try (var arena = Arena.ofConfined()) {
            var lx = arena.allocate(ValueLayout.JAVA_INT);
            var ly = arena.allocate(ValueLayout.JAVA_INT);
            var flag = wlr_scene_node_coords(this.sceneNodePtr, lx, ly);
            return new Coordinates(lx.get(ValueLayout.JAVA_INT, 0), ly.get(ValueLayout.JAVA_INT, 0));
        }
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
    /// @return Found {@link SceneNode} and coordinates relative to the returned node, or NULL if no node
    ///     found
    public @Nullable Tuple3<SceneNode, Double, Double> nodeAt(double lx, double ly) {
        try (var arena = Arena.ofConfined()) {
            var nxPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
            var nyPtr = arena.allocate(ValueLayout.JAVA_DOUBLE);
            var sceneNodePtr = wlr_scene_node_at(this.sceneNodePtr, lx, ly, nxPtr, nyPtr);

            if (!sceneNodePtr.equals(NULL)) {
                return Tuple.of(
                    new SceneNode(sceneNodePtr).toConcreteSceneNode(),
                    nxPtr.get(ValueLayout.JAVA_DOUBLE, 0),
                    nyPtr.get(ValueLayout.JAVA_DOUBLE, 0));
            } else {
                return null;
            }
        }
    }


    /// Immediately destroy the scene-graph node.
    public void destroy() {
        wlr_scene_node_destroy(sceneNodePtr);
    }


    /// Convenience method, not present in wlroots. Converts {@link SceneNode} to a concrete implementation
    public SceneNode toConcreteSceneNode() {
        return switch (getType()) {
            case Tree -> SceneTree.fromNode(this);
            case Rect -> SceneRect.fromNode(this);
            case Buffer -> SceneBuffer.fromNode(this);
        };
    }


    public class ParentIterator implements Iterator<SceneTree> {
        private SceneNode currentNode;


        public ParentIterator() {
            this.currentNode = SceneNode.this;
        }


        @Override
        public boolean hasNext() {
            return currentNode.getParent() != null;
        }


        @Override
        public SceneTree next() {
            var next = currentNode.getParent();
            assert next != null;
            currentNode = next;
            return next;
        }
    }


    public enum Type {
        Tree(WLR_SCENE_NODE_TREE()),
        Rect(WLR_SCENE_NODE_RECT()),
        Buffer(WLR_SCENE_NODE_BUFFER());

        public final int value;


        Type(int value) {
            this.value = value;
        }


        public static Type of(int value) {
            if (value == WLR_SCENE_NODE_TREE()) return Tree;
            if (value == WLR_SCENE_NODE_RECT()) return Rect;
            if (value == WLR_SCENE_NODE_BUFFER()) return Buffer;

            throw new RuntimeException("Invalid enum value from C code for wlr_scene_node_type");
        }
    }
}