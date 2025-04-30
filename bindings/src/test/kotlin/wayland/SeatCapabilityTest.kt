package wayland

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import wayland.SeatCapability.KEYBOARD
import wayland.SeatCapability.POINTER
import wayland.SeatCapability.TOUCH
import java.util.EnumSet


class SeatCapabilityTest : FunSpec({
    context("set to bitfield") {
        test("POINTER") {
            SeatCapability.setToBitfield(EnumSet.of(POINTER)) shouldBeEqual 1
        }
        test("KEYBOARD") {
            SeatCapability.setToBitfield(EnumSet.of(KEYBOARD)) shouldBeEqual 2
        }
        test("TOUCH") {
            SeatCapability.setToBitfield(EnumSet.of(TOUCH)) shouldBeEqual 4
        }

        test("POINTER | KEYBOARD") {
            SeatCapability.setToBitfield(EnumSet.of(POINTER, KEYBOARD)) shouldBeEqual 3
        }
        test("POINTER | TOUCH") {
            SeatCapability.setToBitfield(EnumSet.of(POINTER, TOUCH)) shouldBeEqual 5
        }

        test("POINTER | KEYBOARD | TOUCH") {
            SeatCapability.setToBitfield(EnumSet.of(POINTER, KEYBOARD, TOUCH)) shouldBeEqual 7
        }
    }
})