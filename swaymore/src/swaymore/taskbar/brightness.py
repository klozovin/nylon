from abc import ABC
import re
import shutil
from enum import Enum, auto
import subprocess
import threading
from concurrent.futures import ThreadPoolExecutor

from gi.repository import GLib, Gtk

# TODO: cleanup threads on exit
thread_pool = ThreadPoolExecutor(max_workers=None, thread_name_prefix="swm_")


def threaded(block):
    def wrapper(*args, **kwargs):
        thread_pool.submit(block, *args, **kwargs)

    return wrapper


class IBrightness(ABC):
    @classmethod
    def get_current_brightness(cls) -> tuple[int, int]:
        pass

    @classmethod
    def set_brightness(cls, percent: int) -> None:
        pass


class BrightnessChange(Enum):
    UP = auto()
    DOWN = auto()


class Brightness(Gtk.Label):
    brightness_utility: IBrightness
    brightness_steps: list[tuple[int, int, int]] = [
        [1, 10, 1],
        [10, 20, 2],
        [20, 50, 5],
        [50, 100, 10]
    ]

    def __init__(self):
        super().__init__(label="✲ n/a")
        print(threading.get_ident())

        # Check for brightness setting utility: `brightnessctl` (laptop) or `ddcutil` (desktop)
        if shutil.which("ddcutil") is not None:
            self.brightness_utility = DesktopBrightness()
        elif shutil.which("brightnessctl") is not None:
            self.brightness_utility = LaptopBrightness()
        else:
            raise Exception("No brightness utility found")

        # Show current brightness in widget
        self._update_backlight_level()

    def set_brightness_up(self):
        self._change_brightness_level(BrightnessChange.UP)

    def set_brightness_down(self):
        self._change_brightness_level(BrightnessChange.DOWN)

    @threaded
    def _update_backlight_level(self):
        current, max = self.brightness_utility.get_current_brightness()
        GLib.idle_add(self.set_text, f"✲ {current}")

    @threaded
    def _change_brightness_level(self, change: BrightnessChange):
        current, _ = self.brightness_utility.get_current_brightness()

        # Find appropriate brightness step
        match change:
            case BrightnessChange.UP:
                def filter_fn(x):
                    return x[0] <= current < x[1]
            case BrightnessChange.DOWN:
                def filter_fn(x):
                    return x[0] < current <= x[1]
        try:
            (step_min, step_max, step) = next(filter(filter_fn, self.brightness_steps))
        except StopIteration:
            print("at the end of the range, will not do anything")
            return

        # Calculate new brightness level
        new_brightness: int = 1
        match change:
            case BrightnessChange.UP:
                new_brightness = current + step
            case BrightnessChange.DOWN:
                new_brightness = current - step

        # Clamp the brightness level inside the current step
        if new_brightness > step_max:
            new_brightness = step_max
        elif new_brightness < step_min:
            new_brightness = step_min

        # Change the brightness level and update the UI
        self.brightness_utility.set_brightness(new_brightness)
        current, _ = self.brightness_utility.get_current_brightness()
        GLib.idle_add(lambda: self.set_text(f"✲ {current}"))


class LaptopBrightness(IBrightness):
    executable: str = "brightnessctl"

    def get_current_brightness(self) -> tuple[int, int]:
        backlight_info = (subprocess
                          .run(["brightnessctl", "-m", "info"], capture_output=True)
                          .stdout
                          .decode("utf-8")
                          .strip()
                          .split(","))
        (device, type, current_brightness, percentage, max_brightness) = backlight_info
        current_brightness: int = int(current_brightness)
        percentage: int = int(percentage.strip("%"))
        max_brightness = int(max_brightness)
        return percentage, 100

    def set_brightness(self, percent: int) -> None:
        result = (subprocess
                  .run(["brightnessctl", "-m", "set", f"{percent}%"], capture_output=True)
                  .stdout
                  .decode("utf-8")
                  .strip()
                  .split(","))
        new_percent = int(result[3].strip("%"))
        assert percent == new_percent


class DesktopBrightness(IBrightness):
    executable: str = "ddcutil"
    vcp_brightness: str = "10"  # Should be brightness on most monitors

    def get_current_brightness(self) -> tuple[int, int]:
        result = (subprocess
                  .run([self.executable, "--terse", "getvcp", self.vcp_brightness],
                       capture_output=True)
                  .stdout
                  .decode("utf-8"))
        current, max = re.search("VCP 10 C\s(\d+)\s(\d+)", result).groups()
        return int(current), int(max)

    def set_brightness(self, percent: int) -> None:
        subprocess.run([self.executable, "setvcp", self.vcp_brightness, str(percent)])
