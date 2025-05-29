package compositor

import wlroots.util.Log


fun main(args: Array<String>) {
    Log.init(Log.Importance.DEBUG)
    val compositor = Compositor()
    compositor.start()
}