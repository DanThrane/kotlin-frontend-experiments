package dk.thrane.playground.components

import kotlin.browser.localStorage
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LocalStorageDelegate<R> internal constructor(
    private val name: String?
) : ReadWriteProperty<R, String?> {
    override fun getValue(thisRef: R, property: KProperty<*>): String? {
        return localStorage.getItem(name ?: property.name)
    }

    override fun setValue(thisRef: R, property: KProperty<*>, value: String?) {
        if (value == null) {
            localStorage.removeItem(name ?: property.name)
        } else {
            localStorage.setItem(name ?: property.name, value)
        }
    }
}

object LocalStorage {
    fun delegate(name: String? = null) = LocalStorageDelegate<Any?>(name)
}
