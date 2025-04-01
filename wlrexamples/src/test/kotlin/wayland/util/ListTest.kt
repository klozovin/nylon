package wayland.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import jexwayland.wl_list
import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG


class ListElement(val elementPtr: MemorySegment) : List.Element<ListElement> {

    constructor(arena: Arena) : this(arena.allocate(LAYOUT))

    //
    // IMPORTANT: Don't use Kotlin properties
    //

    fun getX(): Int = LAYOUT.varHandle(groupElement("x")).get(elementPtr, 0) as Int
    fun setX(x: Int) = LAYOUT.varHandle(groupElement("x")).set(elementPtr, 0, x)

    fun getY(): Int = LAYOUT.varHandle(groupElement("y")).get(elementPtr, 0) as Int
    fun setY(y: Int) = LAYOUT.varHandle(groupElement("y")).set(elementPtr, 0, y)

    fun getZ(): Long = LAYOUT.varHandle(groupElement("z")).get(elementPtr, 0) as Long
    fun setZ(z: Long) = LAYOUT.varHandle(groupElement("z")).set(elementPtr, 0, z)

    override fun getLinkPtr(): MemorySegment {
        val linkByteOffset = LAYOUT.byteOffset(groupElement("link"))
        val linkSizeBytes = wl_list.layout().byteSize()
        val linkPtr = elementPtr.asSlice(linkByteOffset, linkSizeBytes)
        return linkPtr
    }

    override fun getCls(): Class<ListElement> = ListElement::class.java

    companion object {
        val LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("x"),
            MemoryLayout.paddingLayout(4),

            wl_list.layout().withName("link"),

            JAVA_INT.withName("y"),
            MemoryLayout.paddingLayout(4),

            JAVA_LONG.withName("z"),
        )

        fun allocate(arena: Arena): ListElement {
            val listElementPtr = arena.allocate(LAYOUT)
            val listElement = ListElement(listElementPtr)
            listElement.setX(100)
            listElement.setY(200)
            listElement.setZ(300)
            return listElement
        }
    }
}


//class ListElementTest : FunSpec({
//
//    context("Bare ListElement node") {
//        Arena.ofConfined().use { arena ->
//            val list = List.allocate<ListElement>(arena, ListElement::class.java)
//
//            test("List should be empty") {
//                list.empty().shouldBeTrue()
//            }
//
//            val element = ListElement.allocate(arena)
//            list.append(element)
//
//            test("List should not be empty") {
//                list.empty().shouldBeFalse()
//            }
//
//            test("List length should be ") {
//                list.length() shouldBeEqual 1
//            }
//        }
//    }
//})


class ListTest : FunSpec({

//    isolationMode = IsolationMode.InstancePerTest
//    beforeEach {}
//    beforeTest {}

    val arena = Arena.global()

    context("Just the list element") {
        val element = ListElement.allocate(arena)
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
    }

    context("List with no elements added") {
        val list = List.allocate<ListElement>(arena, ListElement::class.java)
        list.init()

        test("Should be empty") {
            list.empty().shouldBeTrue()
        }
        test("Length should be 0") {
            list.length() shouldBeEqual 0
        }
        test("List.getNext() should return null") {
            list.getNext().shouldBeNull()
        }
        test("List.getPrev() should return null") {
            list.getPrev().shouldBeNull()
        }
        test("Pointer .prev should point to list head") {
            wl_list.prev(list.listPtr) shouldBeEqual list.listPtr
        }
        test("Pointer .next should point to list head") {
            wl_list.next(list.listPtr) shouldBeEqual list.listPtr
        }
    }

    context("List with single element appended") {
        val list = List.allocate<ListElement>(arena, ListElement::class.java)
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
        test("getNext() and getPrev() return the same object") {
            val prev = list.getPrev()!!
            val next = list.getNext()!!
            prev.getX() shouldBeEqual next.getX()
            prev.getY() shouldBeEqual next.getY()
            prev.getZ() shouldBeEqual next.getZ()
        }
        test("List.Element.next()") {
            val next = element.next()
            element.getX() shouldBeEqual next.getX()
//            element.getY() shouldBeEqual next.getY()
//            element.getZ() shouldBeEqual next.getZ()
        }
    }

//    context("Empty list") {
//        Arena.ofConfined().use { arena ->
//            val list = List<ListElement>(arena)
//            list.init()
//
//            test("wl_list.next should point to list head") {
//                list.listPtr shouldBeEqual wl_list.next(list.listPtr)
//            }
//            test("wl_list.prev should point to list head") {
//                list.listPtr shouldBeEqual wl_list.prev(list.listPtr)
//            }
//
//            test("wl_list should be empty") {
//                list.empty().shouldBeTrue()
//            }
//
//            test("null") {
////                list.getNext().shouldBeNull()
//            }
//        }
//    }

    xcontext("Low level struct init") {
        Arena.ofConfined().use { arena ->
            val layout = MemoryLayout.structLayout(
                JAVA_INT.withName("aaa"),
                MemoryLayout.paddingLayout(4),
                wl_list.layout().withName("link"),
                JAVA_INT.withName("bbb"),
            )
            val struct = arena.allocate(layout)

            test("Layout size") {
                layout.byteSize() shouldBeEqual (4 + 4 + (8 + 8) + 4)
            }
            test("Field 'aaa' offset") {
                layout.byteOffset((groupElement("aaa"))) shouldBeEqual 0
            }
            test("Field 'link' offset") {
                layout.byteOffset((groupElement("link"))) shouldBeEqual (4 + 4)
            }
            test("Field 'bbb' offset") {
                layout.byteOffset((groupElement("bbb"))) shouldBeEqual (4 + 4) + (8 + 8)
            }
            test("Struct size") {
                struct.byteSize() shouldBeEqual (4 + 4) + (8 + 8) + 4
            }
            test("Get/set field 'aaa' directly") {
                struct.set(JAVA_INT, layout.byteOffset(groupElement("aaa")), 100)
                struct.get(JAVA_INT, layout.byteOffset(groupElement("aaa"))) shouldBeEqual 100
            }
            test("Get/set field 'bbb' directly") {
                struct.set(JAVA_INT, layout.byteOffset(groupElement("bbb")), 200)
                struct.get(JAVA_INT, layout.byteOffset(groupElement("aaa"))) shouldBeEqual 100
                struct.get(JAVA_INT, layout.byteOffset(groupElement("bbb"))) shouldBeEqual 200
            }
        }
    }

    xcontext("With Kotlin wrapper class") {
        Arena.ofConfined().use { arena ->
            val element = ListElement(arena)

            test("Set/get element 'x'") {
                element.setX(100)
                element.getX() shouldBeEqual 100
            }

            test("Set/get element 'z'") {
                element.setZ(300)
                element.getX() shouldBeEqual 100
                element.getZ() shouldBeEqual 300
            }
        }
    }

    xcontext("With Java wrapper class") {
        Arena.ofConfined().use { arena ->
            val element = DummyListElement(arena)

            test("Set/get element 'x'") {
                element.x = 100
                element.x shouldBeEqual 100

                element.x = 200
                element.x shouldBeEqual 200
            }

            test("Set/get element 'z'") {
                element.setZ(1000)
                element.z shouldBeEqual 1000

                element.setZ(2000)
                element.z shouldBeEqual 2000
            }
        }
    }

//    xcontext("Custom list element") {
//        Arena.ofConfined().use { arena ->
//            val element = ListElement(arena)
//            test("set/get x property") {
//                element.x = 100L
//                element.x shouldBeEqual 100L
//            }
//        }
//    }

//    context("List with one element") {
//        Arena.ofConfined().use { arena ->
//            val listPtr = arena.allocate(ListElement.LAYOUT)
//            val aHandle = ListElement.LAYOUT.varHandle(groupElement("a"))
//            aHandle.set(listPtr, 10)
//            ValueLayout.JAVA_INT.byteSize()
////            listPtr.set(
////                ListElement.LAYOUT.select(groupElement("a")),
////
////                )
//
//        }
//    }
})