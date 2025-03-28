package wayland.util;

import jexwayland.wl_list;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;

import static jexwayland.util_h.wl_list_insert;


public interface IList<SELF> {
    MemorySegment getLink();


    default IList<SELF> getPrev() {
        var currentElementPtr = getLink();
        var previousElementPtr = wl_list.prev(currentElementPtr);
        return new IList<SELF>() {
            @Override
            public MemorySegment getLink() {
                return previousElementPtr;
            }
        };
    }


    default IList<SELF> getNext() {
        var currentElementPtr = getLink();
        var nextElementPtr = wl_list.next(currentElementPtr);
        return new IList<SELF>() {
            @Override
            public MemorySegment getLink() {
                return nextElementPtr;
            }
        };
    }


    default void append(@NonNull IList<SELF> element) {
        getPrev().insert(element);
    }


    default void insert(IList<SELF> element) {
        wl_list_insert(getLink(), element.getLink());
    }
}