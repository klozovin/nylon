package wayland.util.list2;

import jexwayland.wl_list;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;

import static jexwayland.util_h.wl_list_insert;

interface Proxy {
    @NonNull MemorySegment proxyPtr = null;
}

public interface IList extends Proxy {


    // TODO: Is default implementation possible?
    MemorySegment getLink();


    default MemorySegment getPrev() {
        return wl_list.prev(getLink());
    }


    default MemorySegment getNext() {
        return wl_list.next(getLink());
    }


    default void append(IList element) {
        var lastPtr = getPrev();
        var elementPtr = element.getLink();
        wl_list_insert(lastPtr, elementPtr);
    }


    default void insert(IList element) {
        wl_list_insert(getLink(), element.getLink());
    }
}