package compositor

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