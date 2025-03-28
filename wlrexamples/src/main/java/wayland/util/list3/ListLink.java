package wayland.util.list3;

import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;

public interface ListLink {

    @NonNull MemorySegment getLink();
    
}
