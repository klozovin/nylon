package wayland

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import wayland.SeatCapability.Keyboard
import wayland.SeatCapability.Pointer
import wayland.SeatCapability.Touch
import java.util.EnumSet


class SeatCapabilityTest : FunSpec({
    context("set to bitfield") {
        test("POINTER") {
            SeatCapability.toBitset(EnumSet.of(Pointer)) shouldBeEqual 1
        }

        test("KEYBOARD") {
            SeatCapability.toBitset(EnumSet.of(Keyboard)) shouldBeEqual 2
        }

        test("TOUCH") {
            SeatCapability.toBitset(EnumSet.of(Touch)) shouldBeEqual 4
        }

        test("POINTER | KEYBOARD") {
            SeatCapability.toBitset(EnumSet.of(Pointer, Keyboard)) shouldBeEqual 3
        }

        test("POINTER | TOUCH") {
            SeatCapability.toBitset(EnumSet.of(Pointer, Touch)) shouldBeEqual 5
        }

        test("POINTER | KEYBOARD | TOUCH") {
            SeatCapability.toBitset(EnumSet.of(Pointer, Keyboard, Touch)) shouldBeEqual 7
        }
    }
})