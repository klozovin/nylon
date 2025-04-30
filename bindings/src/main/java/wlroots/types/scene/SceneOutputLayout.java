package wlroots.types.scene;

import java.lang.foreign.MemorySegment;


public class SceneOutputLayout {
    public final MemorySegment sceneOutputLayoutPtr;


    public SceneOutputLayout(MemorySegment sceneOutputLayoutPtr) {
        this.sceneOutputLayoutPtr = sceneOutputLayoutPtr;
    }
}