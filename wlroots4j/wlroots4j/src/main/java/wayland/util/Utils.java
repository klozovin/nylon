package wayland.util;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;


public class Utils {

    /// Given a pointer to a member of a struct, and it's name, compute the pointer of the struct
    /// containing that member.
    ///
    /// Often used with lists, signals and listeners.
    ///
    /// @param memberPtr Pointer to a member of a struct
    /// @param layout    The way the struct looks in memory (specified with Panama)
    /// @param name      Name of the member link (that is, name of the member struct memberPtr points to)
    public static MemorySegment containerOf(MemorySegment memberPtr, GroupLayout layout, String name) {
        var linkMemberByteOffset = layout.byteOffset(groupElement(name));
        var address = memberPtr.address() - linkMemberByteOffset;

        // Have to have a MemorySegment with address and size!
        return MemorySegment.ofAddress(address).reinterpret(layout.byteSize());
    }
}