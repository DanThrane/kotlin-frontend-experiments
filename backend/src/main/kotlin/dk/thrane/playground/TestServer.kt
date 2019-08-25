package dk.thrane.playground

import java.io.ByteArrayOutputStream
import java.io.File

class TestServer : HttpRequestHandler, WebSocketRequestHandler {
    override fun HttpClient.handleRequest(method: HttpMethod, path: String) {
        val rootDir = File(".").normalize().absoluteFile
        val rootDirPath = rootDir.absolutePath + "/"

        if (path == "/favicon.ico") {
            sendHttpResponse(404, defaultHeaders())
        } else if (path.startsWith("/assets/")) {
            val file = File(rootDir, path)
                .normalize()
                .takeIf { it.absolutePath.startsWith(rootDirPath) && it.exists() && it.isFile }

            if (file == null) {
                sendHttpResponse(404, defaultHeaders())
            } else {
                sendHttpResponseWithFile(file)
            }
        } else {
            sendHttpResponseWithFile(File(rootDir, "index.html"))
        }
    }

    override fun HttpClient.handleBinaryFrame(frame: ByteArray) {
        println("new frame!")
        val message = try {
            parseMessage(ByteStreamJVM(frame))
        } catch (ex: Throwable) {
            println("Caught an exception parsing message")
            ex.printStackTrace()

            sendWebsocketFrame(WebSocketOpCode.CONNECTION_CLOSE, ByteArray(0))
            closing = true
            return
        }

        defaultBufferPool.useInstance { buffer ->
            val bound = BoundMessage<TestMessage>(message as ObjectField)
            check(bound[TestMessage.text] == "Hello!")

            val out = ByteOutStreamJVM(buffer)
            writeMessage(out, message)
            println(buffer)
            println(out.ptr)
            sendWebsocketFrame(WebSocketOpCode.BINARY, buffer, 0, out.ptr)
        }
    }
}

fun main() {
    val server = TestServer()
    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
