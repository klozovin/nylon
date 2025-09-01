package wlroots.types;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemorySegment.NULL;


@NullMarked
public class DataSource {
    public final MemorySegment dataSourcePtr;


    public DataSource(MemorySegment dataSourcePtr) {
        assert !dataSourcePtr.equals(NULL);
        this.dataSourcePtr = dataSourcePtr;
    }


    public static @Nullable DataSource ofPtrOrNull(MemorySegment ptr) {
        return !ptr.equals(NULL) ? new DataSource(ptr) : null;
    }
}