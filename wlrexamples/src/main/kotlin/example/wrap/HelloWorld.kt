package example.wrap

import wlroots.Log
import wlroots.Version

fun main() {
    Log.init(Log.Importance.DEBUG)
    Log.log(Log.Importance.INFO, "Hello, world")
    Log.log(Log.Importance.INFO, "This is Java version: ${System.getProperty("java.version")}")
    Log.log(Log.Importance.INFO, "Running on wlroots version: ${Version.STR}")
}