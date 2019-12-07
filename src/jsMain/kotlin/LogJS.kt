package dk.thrane.playground

actual fun printlnWithLogColor(level: LogLevel, message: String) {
    console.log(
        "%c$message",
        when (level) {
            LogLevel.DEBUG -> "color: gray; background: white"
            LogLevel.INFO -> "color: black; background: white"
            LogLevel.WARN -> "color: yellow; background: black"
            LogLevel.ERROR -> "color: red; background: white"
        }
    )
}
