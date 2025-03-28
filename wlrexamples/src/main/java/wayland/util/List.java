package wayland.util;

import jexwayland.wl_list;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;

import static jexwayland.util_h.wl_list_insert;


public final class List<T extends List.Element<T>> {
    public final @NonNull MemorySegment listPtr;
    public final @NonNull Class<T> cls;


    public List(@NotNull MemorySegment listPtr, @NotNull Class<T> cls) {
        this.listPtr = listPtr;
        this.cls = cls;
    }


    public @NonNull T getPrev() {
        var previousElementPtr = wl_list.prev(listPtr);
        return constructElement(previousElementPtr);
    }


    public @NonNull T getNext() {
        var previousElementPtr = wl_list.next(listPtr);
        return constructElement(previousElementPtr);
    }


    public void insert(@NonNull T after, @NonNull T element) {
        wl_list_insert(after.getLinkPtr(), element.getLinkPtr());
    }


    public void append(T element) {
        insert(getPrev(), element);
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


    public interface Element<T extends Element> {
        MemorySegment getLinkPtr();

        Class<T> getCls();

        default T next() {
            try {
                var x = (Class<Element<T>>) getClass();
                return (T) x.getConstructor(MemorySegment.class).newInstance(wl_list.next(getLinkPtr()));

//                return (T) getClass().getConstructor(MemorySegment.class).newInstance(wl_list.next(getLinkPtr()));
//                return getCls().getConstructor(MemorySegment.class).newInstance(wl_list.next(getLinkPtr()));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}