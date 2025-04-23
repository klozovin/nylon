import wlroots.util.Log


object Pointer {
    fun main() {

    }
}


fun main() {
    Log.init(Log.Importance.DEBUG)
    Pointer.main()
}


inline fun error(block: () -> Any): Nothing = error(block())