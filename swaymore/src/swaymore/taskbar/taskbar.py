import threading
from gi.repository import Gtk, GtkLayerShell

from .clock import Clock
from .workspaces import Workspaces
from .brightness import Brightness
# from .battery import Battery
from .volume import Volume


def print_current_thread():
    print(f"Current thread native_id: {threading.current_thread().native_id}")


class TaskbarWindow(Gtk.Window):
    def __init__(self):
        super().__init__()
        self.connect("destroy", Gtk.main_quit)

        # Use wlr_layer_shell to position the taskbar at the bottom of the screen
        GtkLayerShell.init_for_window(self)
        GtkLayerShell.auto_exclusive_zone_enable(self)
        GtkLayerShell.set_anchor(self, GtkLayerShell.Edge.BOTTOM, True)
        GtkLayerShell.set_anchor(self, GtkLayerShell.Edge.LEFT, True)
        GtkLayerShell.set_anchor(self, GtkLayerShell.Edge.RIGHT, True)

        # Box to keep workspace buttons and blocks
        self.box = Gtk.Box()

        # Workspace switcher
        self.workspace_switcher = Workspaces()
        self.box.pack_start(self.workspace_switcher, False, True, 0)

        #
        # Blocks
        #

        # Battery
        # self.battery = Battery()

        # Volume
        self.volume = Volume()

        # Backlight
        self.brightness = Brightness()

        # Clock
        self.clock = Clock()

        # Pack everything and add to window
        self.box.pack_end(self.clock, False, True, 10)
        self.box.pack_end(self.brightness, False, True, 10)
        self.box.pack_end(self.volume, False, True, 10)
        # self.box.pack_end(self.battery, False, True, 10)
        self.add(self.box)
