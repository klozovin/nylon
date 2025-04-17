package example

import wlroots.Log
import wlroots.Version

fun main() {
    Log.init(Log.Importance.DEBUG)
    Log.logSilent("Silent")
    Log.log(Log.Importance.INFO, "Hello, world")
    Log.logError("This is Java version: ${System.getProperty("java.version")}")
    Log.logDebug("Running on wlroots version: ${Version.STR}")
}