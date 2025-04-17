package wlroots.wlr.types;

import org.jspecify.annotations.NullMarked;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class SceneTree {
    public final MemorySegment sceneTreePtr;


    public SceneTree(MemorySegment sceneTreePtr) {
        assert !sceneTreePtr.equals(NULL);
        this.sceneTreePtr = sceneTreePtr;
    }
}