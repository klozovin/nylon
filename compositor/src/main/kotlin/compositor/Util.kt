package compositor


fun unreachable(message: String? = null): Nothing {
    throw IllegalStateException(message ?: "Unreachable code path")
}


inline fun error(block: () -> Any): Nothing =
    error(block())