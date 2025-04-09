package wayland.util;

import jexwayland.wl_list;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;

import static jexwayland.util_h.*;
import static wayland.util.Utils.containerOf;


public final class List<T extends List.Element<@NonNull T>> {
    public final @NonNull MemorySegment listPtr;
    public final @NonNull ElementMetadata<T> meta;


    @Deprecated()
    public List(@NonNull MemorySegment listPtr) {
        this.listPtr = listPtr;
        this.meta = null;
    }


    public List(@NonNull MemorySegment listPtr, List.@NonNull ElementMetadata<T> meta) {
        this.listPtr = listPtr;
        this.meta = meta;
    }


    public static <T extends Element<@NonNull T>> @NonNull List<T> allocate(@NonNull Arena arena, List.@NonNull ElementMetadata<T> meta) {
        var list = new List<>(wl_list.allocate(arena), meta);
        list.init();
        return list;
    }


    ///  Get `wl_list.prev` field.
    public @NonNull MemorySegment getPrev() {
        return wl_list.prev(listPtr);
    }


    /// Get `wl_list.next` field
    public @NonNull MemorySegment getNext() {
        return wl_list.next(listPtr);
    }


    public @Nullable T getFirst() {
        var nextElementLinkPtr = getNext();

        // wl_list.next points to list head => List is empty
        if (nextElementLinkPtr.equals(listPtr))
            return null;

        var firstPtr = containerOf(nextElementLinkPtr, meta.layout, meta.linkMemberName);
        return constructElement(firstPtr);
    }


    public @Nullable T getLast() {
        var lastElementLinkPtr = getPrev();

        // wl_list.prev points to list head => List is empty
        if (lastElementLinkPtr.equals(listPtr))
            return null;

        var lastPtr = containerOf(lastElementLinkPtr, meta.layout, meta.linkMemberName);
        return constructElement(lastPtr);
    }


    /// Initializes the list.
    public void init() {
        wl_list_init(listPtr);
    }


    /// Determines if the list is empty.
    public boolean empty() {
        return switch (wl_list_empty(listPtr)) {
            case 0 -> false;
            case 1 -> true;
            default -> throw new IllegalStateException("Unexpected from wl_list_empty(): " + wl_list_empty(listPtr));
        };
    }


    /// Determines the length of the list.
    public int length() {
        return wl_list_length(listPtr);
    }


    /// Insert `element` after `after` element.
    public void insert(@NonNull T after, @NonNull T element) {
        wl_list_insert(after.getLinkMemberPtr(), element.getLinkMemberPtr());
    }


    ///  Add `element` to the end of the list.
    public void append(T element) {
        var previous = getLast();
        if (previous != null) {
            wl_list_insert(previous.getLinkMemberPtr(), element.getLinkMemberPtr());
        } else {
            // List is empty, add to list head itself
            wl_list_insert(listPtr, element.getLinkMemberPtr());
        }
    }


    private T constructElement(@NonNull MemorySegment elementPtr) {
        try {
            var element = meta.elementClass.getConstructor(MemorySegment.class).newInstance(elementPtr);
            return element;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public interface Element<T extends Element<T>> {
        MemorySegment getLinkMemberPtr();

        default @NonNull MemorySegment getNext() {
            return wl_list.next(getLinkMemberPtr());
        }

        default @NonNull MemorySegment getPrev() {
            return wl_list.prev(getLinkMemberPtr());
        }
    }

    public record ElementMetadata<T extends List.Element<@NonNull T>>(
        @NonNull Class<T> elementClass,
        @NonNull GroupLayout layout,
        @NonNull String linkMemberName
    ) {
    }
}