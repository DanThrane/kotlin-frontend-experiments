package dk.thrane.playground.io

import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun AsynchronousServerSocketChannel.asyncAccept() = suspendCoroutine<AsynchronousSocketChannel> { cont ->
    accept(null, object : CompletionHandler<AsynchronousSocketChannel, Nothing?> {
        override fun completed(result: AsynchronousSocketChannel, attachment: Nothing?) {
            cont.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Nothing?) {
            cont.resumeWithException(exc)
        }
    })
}

suspend fun AsynchronousSocketChannel.asyncRead(buffer: ByteBuffer) = suspendCoroutine<Int> { cont ->
    read(buffer, null, object : CompletionHandler<Int, Nothing?> {
        override fun completed(result: Int, attachment: Nothing?) {
            cont.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Nothing?) {
            cont.resumeWithException(exc)
        }
    })
}

suspend fun AsynchronousSocketChannel.asyncWrite(buffer: ByteBuffer) = suspendCoroutine<Int> { cont ->
    write(buffer, null, object : CompletionHandler<Int, Nothing?> {
        override fun completed(result: Int, attachment: Nothing?) {
            cont.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Nothing?) {
            cont.resumeWithException(exc)
        }
    })
}

suspend fun AsynchronousSocketChannel.asyncConnect(socketAddress: SocketAddress) = suspendCoroutine<Unit> { cont ->
    connect(socketAddress, null, object : CompletionHandler<Void, Nothing?> {
        override fun completed(result: Void?, attachment: Nothing?) {
            cont.resume(Unit)
        }

        override fun failed(exc: Throwable, attachment: Nothing?) {
            cont.resumeWithException(exc)
        }
    })
}
