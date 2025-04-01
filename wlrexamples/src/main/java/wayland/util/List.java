package wayland.util;

import jexwayland.wl_list;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;

import static jexwayland.util_h.*;


public final class List<T extends List.Element<@NonNull T>> {
    public final @NonNull MemorySegment listPtr;
    public final @NonNull Class<T> cls;


    public List(@NonNull Arena arena) {
        this.listPtr = wl_list.allocate(arena);
        this.cls = null;
    }


    @Deprecated()
    public List(@NonNull MemorySegment listPtr, @NonNull Class<T> cls) {
        this.listPtr = listPtr;
        this.cls = cls;
    }


    /// Allocate and initialize new `wl_list` object.
    public static <T extends Element<@NonNull T>> List<T> allocate(@NonNull Arena arena, @NonNull Class<T> cls) {
        var list = new List<>(wl_list.allocate(arena), cls);
        list.init();
        return list;
    }


    ///  Get `wl_list.prev` field.
    ///
    /// Return null when the list is empty.
    public @Nullable T getPrev() {
        var previousPtr = wl_list.prev(listPtr);
        if (previousPtr.equals(listPtr)) return null;
        return constructElement(previousPtr);
    }


    /// Get `wl_list.next` field
    ///
    /// Return null when the list is empty.
    public @Nullable T getNext() {
        var nextPtr = wl_list.next(listPtr);
        if (nextPtr.equals(listPtr)) return null;
        return constructElement(nextPtr);
    }


    /// Initializes the list.
    public void init() {
        wl_list_init(listPtr);
    }


    /// Determines if the list is empty.
    ///
    /// Note: should work both on
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
        wl_list_insert(after.getLinkPtr(), element.getLinkPtr());
    }


    public void append(T element) {
        var previous = getPrev();
        if (previous != null) {
            insert(getPrev(), element);
        } else {
            // List is empty, add to list head itself
            System.out.println("it emptyyyyyyy");
            wl_list_insert(listPtr, element.getLinkPtr());
        }
    }


    private T constructElement(@NonNull MemorySegment elementPtr) {
        try {
            var element = cls.getConstructor(MemorySegment.class).newInstance(elementPtr);
            return element;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public interface Element<T extends Element<T>> {
        MemorySegment getLinkPtr();

        @NonNull
        Class<T> getCls();

        default @NonNull T next() {
            try {
                var cls = (Class<Element<T>>) getClass();
                var ctor = cls.getConstructor(MemorySegment.class);

                TODO

//                        // calc struct beginning

                var element = ctor.newInstance(wl_list.next(getLinkPtr()));

                return (T) element;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}