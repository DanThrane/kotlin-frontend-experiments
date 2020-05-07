package dk.thrane.playground.serialization

actual class ByteArrayPool actual constructor(private val generator: () -> ByteArray, numberOfElements: Int) {
    actual fun borrowInstance(): Pair<Int, ByteArray> {
        return Pair(0, generator())
    }

    actual fun returnInstance(id: Int) {

    }
}

