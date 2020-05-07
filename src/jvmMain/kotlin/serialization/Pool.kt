package dk.thrane.playground.serialization

import dk.thrane.playground.ObjectPool

actual class ByteArrayPool actual constructor(generator: () -> ByteArray, numberOfElements: Int) {
    private val delegate = ObjectPool(numberOfElements, generator, reset = {}, isValid = { true })

    actual fun borrowInstance(): Pair<Int, ByteArray> {
        val (arr, id) = delegate.borrowInstance()
        return Pair(id, arr)
    }

    actual fun returnInstance(id: Int) {
        delegate.returnInstance(id)
    }
}
