package dk.thrane.playground

typealias StateListener<T> = (T) -> Unit

class State<T>(initialValue: T, private val compareWithOldValue: Boolean) {
    var current: T = initialValue
        set(value) {
            if (!compareWithOldValue || value != field) {
                field = value
                subscriptions.forEach { it(value) }
            }
        }
    private val subscriptions = ArrayList<StateListener<T>>()

    fun listen(listener: StateListener<T>): StateListener<T> {
        subscriptions.add(listener)
        listener(current)
        return listener
    }

    fun removeListener(listener: StateListener<T>) {
        subscriptions.remove(listener)
    }
}

fun <T, R> State<T>.map(compareWithOldValue: Boolean = true, mapper: (T) -> R): State<R> {
    val resultState = State(mapper(current), compareWithOldValue)
    listen { resultState.current = mapper(it) }
    return resultState
}

fun <T> stateOf(initialValue: T, compareWithOldValue: Boolean = true): State<T> {
    return State(initialValue, compareWithOldValue)
}

interface StateListListener<T> {
    fun onAdd(item: T, idx: Int)
    fun onRemove(item: T, idx: Int)
    fun onClear()
}

class StateList<T>(initialItems: Collection<T>) : Iterable<T> {
    private val list = ArrayList(initialItems)
    private val listeners = ArrayList<StateListListener<T>>()

    fun addListener(listener: StateListListener<T>): StateListListener<T> {
        listeners.add(listener)
        list.forEachIndexed { idx, item -> listener.onAdd(item, idx) }
        return listener
    }

    fun removeListener(listener: StateListListener<T>) {
        listeners.remove(listener)
    }

    fun add(item: T) {
        list.add(item)
        listeners.forEach { it.onAdd(item, list.lastIndex) }
    }

    fun addBefore(idx: Int, item: T) {
        list.add(idx, item)
        listeners.forEach { it.onAdd(item, idx) }
    }

    fun remove(item: T) {
        val idx = list.indexOf(item)
        if (idx != -1) {
            listeners.forEach { it.onRemove(item, idx) }
        }
    }

    fun remove(predicate: (T) -> Boolean) {
        val iterator = list.iterator()
        var idx = 0
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (predicate(next)) {
                listeners.forEach { it.onRemove(next, idx) }
                iterator.remove()
            } else {
                idx++
            }
        }
    }

    fun removeAt(idx: Int) {
        require(idx in list.indices) { "Index out of bounds ($idx !in ${list.indices})" }

        val item = list.removeAt(idx)
        listeners.forEach { it.onRemove(item, idx) }
    }

    fun replace(newCollection: Collection<T>) {
        clear()
        for (item in newCollection) {
            add(item)
        }
    }

    fun clear() {
        list.clear()
        listeners.forEach { it.onClear() }
    }

    override fun iterator(): Iterator<T> = list.iterator()
}

fun <T> stateListOf(vararg items: T): StateList<T> {
    return StateList(arrayListOf(*items))
}