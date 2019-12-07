package dk.thrane.playground

actual fun printlnWithLogColor(level: LogLevel, message: String) {
    console.log(
        "%c$message",
        when (level) {
            LogLevel.DEBUG -> "color: gray"
            LogLevel.INFO -> "color: black"
            LogLevel.WARN -> "color: yellow"
            LogLevel.ERROR -> "color: red"
        }
    )
}
