package compositor

import wayland.server.EventLoop
import wayland.server.EventSource
import java.lang.foreign.Arena
import java.util.EnumSet


fun unreachable(message: String? = null): Nothing {
    throw IllegalStateException(message ?: "This code path should NOT be reachable")
}


inline fun <reified T: Enum<T>> enumSetOf(vararg enums: T): EnumSet<T> {
    val set = EnumSet.noneOf(T::class.java)
    set.addAll(enums)
    return set
}


inline fun error(block: () -> Any): Nothing =
    error(block())


abstract class Timer(eventLoop: EventLoop) {
    private val arena = Arena.ofShared() // TODO: Why not confined?
    private val eventSource: EventSource
    private var delay: Int = 0
    private var removed = false


    init {
        eventSource = eventLoop.addTimer(arena, ::loop)
    }


    abstract fun callback()


    fun start(delayMs: Int) {
        require(!removed)
        delay = delayMs
        eventSource.timerUpdate(delay)

    }


    fun stop() {
        require(!removed)
        delay = 0
        eventSource.timerUpdate(delay)
    }


    fun cleanup() {
        stop()
        eventSource.remove()
        arena.close()
        removed = true
    }


    private fun loop(): Int {
        require(!removed)
        callback()
        eventSource.timerUpdate(delay)
        return 0
    }
}