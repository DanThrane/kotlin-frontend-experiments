package dk.thrane.playground.modules

class AttributeStore {
    private val store = HashMap<String, Any?>()

    operator fun <Value> set(key: AttributeKey<Value>, value: Value) {
        store[key.name] = value
    }

    operator fun <Value> get(key: AttributeKey<Value>): Value {
        @Suppress("UNCHECKED_CAST")
        return (store[key.name] as Value)!!
    }

    fun <Value> getOrNull(key: AttributeKey<Value>): Value? {
        @Suppress("UNCHECKED_CAST")
        return store[key.name] as Value?
    }
}

class AttributeKey<ValueType>(val name: String)
