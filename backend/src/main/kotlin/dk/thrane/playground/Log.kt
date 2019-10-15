package dk.thrane.playground

class Log(val tag: String) {
    fun debug(message: String) {
        LogManager.log(LogLevel.DEBUG, tag, message)
    }

    fun info(message: String) {
        LogManager.log(LogLevel.INFO, tag, message)
    }

    fun warn(message: String) {
        LogManager.log(LogLevel.WARN, tag, message)
    }

    fun error(message: String) {
        LogManager.log(LogLevel.ERROR, tag, message)
    }
}

enum class LogLevel(val short: String) {
    DEBUG("D"),
    INFO("I"),
    WARN("WARNING"),
    ERROR("ERROR")
}

object LogManager {
    var currentLogLevel: LogLevel = LogLevel.DEBUG
    val customLogLevels: MutableMap<String, LogLevel> = HashMap()

    fun log(level: LogLevel, tag: String, message: String) {
        val minLevel = customLogLevels[tag] ?: currentLogLevel
        if (level.ordinal < minLevel.ordinal) return
        val color = when (level) {
            LogLevel.DEBUG -> ANSI_WHITE
            LogLevel.INFO -> ANSI_GREEN
            LogLevel.WARN -> ANSI_YELLOW
            LogLevel.ERROR -> ANSI_RED
        }
        println("$color[${level.short}/$tag] $message${ANSI_RESET}")
    }

    private val ANSI_RESET = "\u001B[0m"
    private val ANSI_BLACK = "\u001B[30m"
    private val ANSI_RED = "\u001B[31m"
    private val ANSI_GREEN = "\u001B[32m"
    private val ANSI_YELLOW = "\u001B[33m"
    private val ANSI_BLUE = "\u001B[34m"
    private val ANSI_PURPLE = "\u001B[35m"
    private val ANSI_CYAN = "\u001B[36m"
    private val ANSI_WHITE = "\u001B[37m"
}
