package wayland.util.list1;

import jexwayland.wl_list;
import org.jspecify.annotations.NonNull;
import wayland.util.list3.ListLink;

import java.lang.foreign.MemorySegment;

import static jexwayland.util_h.wl_list_insert;


/// Doubly-linked list (or a list element).
///
/// On its own, an instance of struct wl_list represents the sentinel head of a doubly-linked list.
///
/// When empty, the list head's next and prev members point to the list head itself, otherwise next
/// references the first element in the list, and prev refers to the last element in the list.
public final class List {
    public final @NonNull MemorySegment listPtr;


    public List(@NonNull MemorySegment listPtr) {
        this.listPtr = listPtr;
    }

    /// Previous list element.
    public @NonNull List getPrev() {
        return new List(wl_list.prev(listPtr));
    }


    /// Next list element.
    public @NonNull List getNext() {
        return new List(wl_list.next(listPtr));
    }


    /// Append an element to the end of the list.
    ///
    ///  Just for convenience, not a direct wrapper of a C API.
    public void append(@NonNull List element) {
        getPrev().insert(element);
    }

    public void append(@NonNull ListLink element) {
        getPrev().insert(new List(element.getLink()));
    }


    /// Inserts an element into the list, after the element represented by list [List#listPtr].
    ///
    /// When list is a reference to the list itself (the head), set the containing struct of elm as
    ///  the first element in the list.
    public void insert(@NonNull List element) {
        wl_list_insert(listPtr, element.listPtr);
    }
}