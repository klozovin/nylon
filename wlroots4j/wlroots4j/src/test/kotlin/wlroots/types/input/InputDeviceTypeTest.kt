package wlroots.types.input

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import jextract.wlroots.wlr.WLR_INPUT_DEVICE_KEYBOARD
import jextract.wlroots.wlr.WLR_INPUT_DEVICE_POINTER
import jextract.wlroots.wlr.WLR_INPUT_DEVICE_SWITCH


class SeatCapabilityTest : FunSpec({
    context("Get enumeration from C int") {
        test("WLR_INPUT_DEVICE_KEYBOARD") {
            InputDeviceType.of(WLR_INPUT_DEVICE_KEYBOARD()) shouldBeEqual InputDeviceType.Keyboard
        }

        test("WLR_INPUT_DEVICE_POINTER") {
            InputDeviceType.of(WLR_INPUT_DEVICE_POINTER()) shouldBeEqual InputDeviceType.Pointer
        }

        test("WLR_INPUT_DEVICE_SWITCH") {
            InputDeviceType.of(WLR_INPUT_DEVICE_SWITCH()) shouldBeEqual InputDeviceType.Switch
        }
    }
})
