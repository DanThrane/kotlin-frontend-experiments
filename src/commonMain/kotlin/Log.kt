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
        printlnWithLogColor(level, "[${level.short}/$tag] $message")
    }
}

expect fun printlnWithLogColor(level: LogLevel, message: String)
