package compositor


fun unreachable(message: String? = null): Nothing {
    throw IllegalStateException(message ?: "Unreachable code path")
}