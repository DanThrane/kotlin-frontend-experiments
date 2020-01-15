package dk.thrane.playground

import kotlin.time.ClockMark
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock

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

    fun message(level: LogLevel, message: String) {
        LogManager.log(level, tag, message)
    }
}

enum class LogLevel(val short: String) {
    DEBUG("D"),
    INFO("I"),
    WARN("WARNING"),
    ERROR("ERROR")
}

object LogManager {
    private var lastLog: ClockMark? = null
    var currentLogLevel: LogLevel = LogLevel.DEBUG
    val customLogLevels: MutableMap<String, LogLevel> = HashMap()

    fun log(level: LogLevel, tag: String, message: String) {
        val minLevel = customLogLevels[tag] ?: currentLogLevel
        if (level.ordinal < minLevel.ordinal) return

        val duration = lastLog?.elapsedNow() ?: Duration.ZERO
        lastLog = MonoClock.markNow()
        printlnWithLogColor(level, "[${level.short}/$tag ${duration}] $message")
    }
}

expect fun printlnWithLogColor(level: LogLevel, message: String)
