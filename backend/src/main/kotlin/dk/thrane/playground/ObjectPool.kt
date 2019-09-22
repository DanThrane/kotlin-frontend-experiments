package dk.thrane.playground

class ObjectPool<T> @PublishedApi internal constructor(
    private val itemGenerator: () -> T,
    private val instances: Array<T>,
    private val reset: (T) -> Unit,
    private val isValid: (T) -> Boolean
) {
    private val lock = Object()
    private val instanceLocks = BooleanArray(instances.size) { false }

    fun borrowInstance(): Pair<T, Int> {
        synchronized(lock) {
            var firstFree: Int = -1
            while (firstFree == -1) {
                firstFree = instanceLocks.indexOf(false)
                if (firstFree == -1) {
                    lock.wait()
                }
            }

            instanceLocks[firstFree] = true
            return Pair(instances[firstFree], firstFree)
        }
    }

    fun returnInstance(idx: Int) {
        synchronized(lock) {
            val instance = instances[idx]
            if (!isValid(instance)) {
                instances[idx] = itemGenerator()
            } else {
                reset(instance)
            }

            instanceLocks[idx] = false
            lock.notifyAll()
        }
    }
}

inline fun <T, R> ObjectPool<T>.useInstance(block: (T) -> R): R {
    val (instance, key) = borrowInstance()
    try {
        return block(instance)
    } finally {
        returnInstance(key)
    }
}

inline fun <reified T> ObjectPool(
    size: Int,
    noinline itemGenerator: () -> T,
    noinline reset: (T) -> Unit,
    noinline isValid: (T) -> Boolean = { true }
): ObjectPool<T> {
    return ObjectPool(itemGenerator, Array(size) { itemGenerator() }, reset, isValid)
}

val defaultBufferPool by lazy { ObjectPool(512, { ByteArray(1024 * 128) }, {}) }