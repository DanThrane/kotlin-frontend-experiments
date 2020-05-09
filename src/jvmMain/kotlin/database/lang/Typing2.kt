package dk.thrane.playground.database.lang

import java.lang.annotation.ElementType
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// TODO Should this be used in the variable declarations directly? This needs to play ball with the Kotlin
//  type system for improved usability.

// Maybe we could replace the entire existing database system to integrate better with this
sealed class Type() {
    object Int8 : Type()
    object Int16 : Type()
    object Int32 : Type()
    object Int64 : Type()
    object Bool : Type()
    object Text : Type()

    class Array<Element : Type>(val elementType: Element) : Type()

    abstract class Object<Self : Object<Self>> : Type() {
        private val fields = ArrayList<Field<*, *>>()
        val allFields: List<Field<*, *>> = fields

        fun <T : Type> addField(name: String, type: T): Field<Self, T> {
            @Suppress("UNCHECKED_CAST")
            val element = Field(name, type, this as Self) // Why doesn't this just work?
            fields.add(element)
            return element
        }

        companion object {
            class Field<Parent: Object<Parent>, T : Type>(
                val name: String,
                val type: T,
                val parent: Parent
            )
        }
    }

    class AssociativeArray<Key : Type, Element : Type>(
        val keyType: Key,
        val elementType: Element
    ) : Type()

    class Optional<Element : Type>(val elementType: Element) : Type()
}

fun <P : Type.Object<P>, T : Type> field(type: T): ReadOnlyProperty<P, Type.Object.Companion.Field<P, T>> {
    return object : ReadOnlyProperty<Type.Object<P>, Type.Object.Companion.Field<P, T>> {
        var didAdd = false

        override fun getValue(thisRef: Type.Object<P>, property: KProperty<*>): Type.Object.Companion.Field<P, T> {
            if (!didAdd) {
                didAdd = true
                return thisRef.addField(property.name, type)
            }

            @Suppress("UNCHECKED_CAST")
            return thisRef.allFields.find { it.name == property.name }!! as Type.Object.Companion.Field<P, T>
        }
    }
}

abstract class Expr<T : Type> {
    abstract val resolvedType: T
}

data class VarRef<E : Type>(
    val name: String,
    val type: E
) : Expr<E>() {
    override val resolvedType: E = type
}

data class FieldAccess2<T : Type.Object<T>, F : Type>(
    val objectAccess: Expr<T>,
    val field: Type.Object.Companion.Field<T, F>
) : Expr<F>() {
    override val resolvedType: F = field.type
}

data class NullSafeFieldAccess<T : Type.Object<T>, F : Type>(
    val objectAccess: Expr<Type.Optional<T>>,
    val field: Type.Object.Companion.Field<T, F>
) : Expr<Type.Optional<F>>() {
    override val resolvedType: Type.Optional<F> = Type.Optional(field.type)
}

data class NotNullAssertion<T : Type>(
    val expr: Expr<Type.Optional<T>>
) : Expr<T>() {
    override val resolvedType: T = expr.resolvedType.elementType
}

fun <T : Type> variable2(type: T): ReadOnlyProperty<Any?, VarRef<T>> {
    return object : ReadOnlyProperty<Any?, VarRef<T>> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): VarRef<T> {
            return VarRef(property.name, type)
        }
    }
}

operator fun <T : Type.Object<T>, F : Type> Expr<T>.get(
    block: T.() -> Type.Object.Companion.Field<T, F>
): FieldAccess2<T, F> {
    return FieldAccess2(this, resolvedType.block())
}

fun <T : Type.Object<T>, F : Type> Expr<Type.Optional<T>>.nullSafeGet(
    block: T.() -> Type.Object.Companion.Field<T, F>
): NullSafeFieldAccess<T, F> {
    return NullSafeFieldAccess(this, resolvedType.elementType.block())
}

fun <T : Type> Expr<Type.Optional<T>>.notNullAssertion(): Expr<T> {
    return NotNullAssertion(this)
}

// Testing

object Testing : Type.Object<Testing>() {
    val f by field(Int8)
    val b by field(Int16)
    val recursive by field(Testing)
}

object CircularA : Type.Object<CircularA>() {
    val b by field(CircularB)
}

object CircularB : Type.Object<CircularB>() {
    val a by field(CircularA)
}

fun main() {
    val t by variable2(Testing)
    val circular by variable2(Type.Optional(CircularA))

    val recursion = t[{ recursive }][{ recursive }][{ recursive }][{ recursive }][{ f }]
    val recursion2 = t.get { recursive }.get { recursive }.get { recursive }.get { recursive }.get { f }


    val nullSafeGet = circular.nullSafeGet { b }.nullSafeGet { a }.nullSafeGet { b }.notNullAssertion().get { a }
    println("Hello!")

}