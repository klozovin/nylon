package wayland.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import jextract.wayland.util.wl_list
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.util.*


class ListElement(val elementPtr: MemorySegment) : List.Element<ListElement> {

    //
    // IMPORTANT: Don't use Kotlin properties
    //

    fun getX(): Int = LAYOUT.varHandle(groupElement("x")).get(elementPtr, 0) as Int
    fun setX(x: Int) = LAYOUT.varHandle(groupElement("x")).set(elementPtr, 0, x)

    fun getY(): Int = LAYOUT.varHandle(groupElement("y")).get(elementPtr, 0) as Int
    fun setY(y: Int) = LAYOUT.varHandle(groupElement("y")).set(elementPtr, 0, y)

    fun getZ(): Long = LAYOUT.varHandle(groupElement("z")).get(elementPtr, 0) as Long
    fun setZ(z: Long) = LAYOUT.varHandle(groupElement("z")).set(elementPtr, 0, z)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ListElement) return false
        return this.getX() == other.getX() && this.getY() == other.getY() && this.getZ() == other.getZ()
    }

    override fun hashCode(): Int {
        return Objects.hash(getX(), getY(), getZ())
    }

    override fun getLinkMemberPtr(): MemorySegment {
        val linkByteOffset = LAYOUT.byteOffset(groupElement("link"))
        val linkSizeBytes = wl_list.layout().byteSize()
        val linkPtr = elementPtr.asSlice(linkByteOffset, linkSizeBytes)
        return linkPtr
    }

    companion object {

        val LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("x"),
            MemoryLayout.paddingLayout(4),

            wl_list.layout().withName("link"),

            JAVA_INT.withName("y"),
            MemoryLayout.paddingLayout(4),

            JAVA_LONG.withName("z"),
        )

        fun allocate(arena: Arena, x: Int = 100, y: Int = 200, z: Long = 300): ListElement {
            val listElementPtr = arena.allocate(LAYOUT)
            val listElement = ListElement(listElementPtr)
            listElement.setX(x)
            listElement.setY(y)
            listElement.setZ(z)
            return listElement
        }

        val meta = List.ElementMetadata(
            ListElement::class.java,
            LAYOUT,
            "link"
        )
    }
}


class ListTest : FunSpec({
//    isolationMode = IsolationMode.InstancePerTest
//    beforeEach {}
//    beforeTest {}

    val arena = Arena.global()

    context("Just the list element") {
        val element = ListElement.allocate(arena)
        val el1 = ListElement.allocate(arena)
        val el2 = ListElement.allocate(arena, 101, 201, 301)
        val el3 = ListElement.allocate(arena, 101, 201, 301)

        test("Layout size") {
            ListElement.LAYOUT.byteSize() shouldBeEqual (4 + 4 + (8 + 8) + (4 + 4) + 8)
        }
        test("Default values") {
            element.getX() shouldBeEqual 100
            element.getY() shouldBeEqual 200
            element.getZ() shouldBeEqual 300
        }
        test("Set/get values") {
            element.setX(101)
            element.getX() shouldBeEqual 101
            element.setY(201)
            element.getY() shouldBeEqual 201
            element.setZ(301)
            element.getZ() shouldBeEqual 301
        }
        test("Equality") {
            el1 shouldBeEqual el1
            el2 shouldBeEqual el3
            el1 shouldNotBeEqual el2
            el1 shouldNotBeEqual el3
        }
    }


    context("List with no elements added") {
        val list = List.allocate<ListElement>(arena, ListElement.meta)
        list.init()

        test("Should be empty") {
            list.empty().shouldBeTrue()
        }
        test("Length should be 0") {
            list.length() shouldBeEqual 0
        }
        test("List.getFirst() should return null") {
            list.first.shouldBeNull()
        }
        test("List.getLast() should return null") {
            list.last.shouldBeNull()
        }
        test("List.prev should point to list head") {
            list.prev() shouldBeEqual list.listPtr
        }
        test("Pointer .next should point to list head") {
            list.next() shouldBeEqual list.listPtr
        }
    }


    context("List with a single element") {
        val list = List.allocate<ListElement>(arena, ListElement.meta)
        list.init()
        val element = ListElement.allocate(arena)

        test("Default list element fields before insertion") {
            element.getX() shouldBeEqual 100
            element.getY() shouldBeEqual 200
            element.getZ() shouldBeEqual 300
        }

        // Append the element
        list.append(element)

        test("Should not be empty") {
            list.empty().shouldBeFalse()
        }
        test("Length should be 1") {
            list.length() shouldBeEqual 1
        }
        test("Default list element fields after insertion") {
            element.getX() shouldBeEqual 100
            element.getY() shouldBeEqual 200
            element.getZ() shouldBeEqual 300
        }
        test("List.prev and List.next point to the same element") {
            list.prev() shouldBeEqual element.linkMemberPtr
            list.next() shouldBeEqual element.linkMemberPtr
        }
        test("List.first is structurally the same object as the inserted element") {
            val first = list.first!!
            first.getX() shouldBeEqual element.getX()
            first.getY() shouldBeEqual element.getY()
            first.getZ() shouldBeEqual element.getZ()
            first shouldBeEqual element
        }
        test("List.last is structurally the same object as the inserted element") {
            val last = list.last!!
            last.getX() shouldBeEqual element.getX()
            last.getY() shouldBeEqual element.getY()
            last.getZ() shouldBeEqual element.getZ()
            last shouldBeEqual element
        }
    }


    context("List with multiple elements") {
        val element1 = ListElement.allocate(arena)
        val element2 = ListElement.allocate(arena, 101, 201, 301)
        val element3 = ListElement.allocate(arena, 102, 202, 302)

        val list = List.allocate<ListElement>(arena, ListElement.meta).apply {
            init()
            append(element1)
            insert(element1, element2)
            append(element3)
        }

        test("Should not be empty") {
            list.empty().shouldBeFalse()
        }
        test("List length") {
            list.length() shouldBeEqual 3
        }
        test("Element 1 retrieval") {
            list.first!! shouldBeEqual element1
        }
        test("Element 3 retrieval") {
            list.last!! shouldBeEqual element3
        }
        test("List.next pointers") {
            list.next() shouldBeEqual element1.linkMemberPtr
            element1.next shouldBeEqual element2.linkMemberPtr
            element2.next shouldBeEqual element3.linkMemberPtr
            element3.next shouldBeEqual list.listPtr
        }
        test("List.prev pointers") {
            list.prev() shouldBeEqual element3.linkMemberPtr
            element3.prev shouldBeEqual element2.linkMemberPtr
            element2.prev shouldBeEqual element1.linkMemberPtr
            element1.prev shouldBeEqual list.listPtr
        }
    }
})