package dk.thrane.playground

actual fun printlnWithLogColor(level: LogLevel, message: String) {
    val color = when (level) {
        LogLevel.DEBUG -> ANSI_WHITE
        LogLevel.INFO -> ANSI_GREEN
        LogLevel.WARN -> ANSI_YELLOW
        LogLevel.ERROR -> ANSI_RED
    }

    println(color + message + ANSI_RESET)
}
private val ANSI_RESET = "\u001B[0m"
private val ANSI_RED = "\u001B[31m"
private val ANSI_GREEN = "\u001B[32m"
private val ANSI_YELLOW = "\u001B[33m"
private val ANSI_WHITE = "\u001B[37m"
