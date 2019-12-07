package dk.thrane.playground

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.stackTraceToString(): String {
    val stringWriter = StringWriter()
    printStackTrace(PrintWriter(stringWriter))
    return stringWriter.toString()
}
