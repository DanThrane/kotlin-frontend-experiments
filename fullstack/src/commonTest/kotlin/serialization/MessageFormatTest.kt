package serialization

import dk.thrane.playground.serialization.MessageFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlin.test.*

@Serializable
data class Node(
    val next: Node?,
    val value: Int
)

@Serializable
data class Wrapper(val value: Int)

@Serializable
data class Generic<T>(val value: T)

@Serializable
data class ListOfGeneric<T>(val dummy: Int, val list: List<T>)

@Serializable
data class NullableGeneric<T>(val value: T?)

@Serializable
data class PrimitiveNullable(val value: String?)

@Serializable
data class EnumTest(val enum: MyEnum)

enum class MyEnum {
    A, B, C
}

class MessageFormatTest {
    @Test
    fun testSimpleWrapper() {
        val wrapper = Wrapper(42)
        assertEquals(
            wrapper,
            MessageFormat.decodeFromByteArray(
                Wrapper.serializer(),
                MessageFormat.encodeToByteArray(Wrapper.serializer(), wrapper)
            )
        )
    }

    @Test
    fun testRecursiveStructure() {
        val wrapper = Node(
            Node(
                Node(
                    Node(
                        null,
                        4
                    ),
                    3
                ),
                2
            ),
            1
        )

        assertEquals(
            wrapper,
            MessageFormat.decodeFromByteArray(
                Node.serializer(),
                MessageFormat.encodeToByteArray(Node.serializer(), wrapper)
            )
        )
    }

    @Test
    fun testGeneric() {
        val wrapper = Generic(Wrapper(42))

        assertEquals(
            wrapper,
            MessageFormat.decodeFromByteArray(
                Generic.serializer(Wrapper.serializer()),
                MessageFormat.encodeToByteArray(Generic.serializer(Wrapper.serializer()), wrapper)
            )
        )
    }

    @Test
    fun testListOfGeneric() {
        val wrapper = ListOfGeneric(1337, listOf(Wrapper(42)))

        assertEquals(
            wrapper,
            MessageFormat.decodeFromByteArray(
                ListOfGeneric.serializer(Wrapper.serializer()),
                MessageFormat.encodeToByteArray(ListOfGeneric.serializer(Wrapper.serializer()), wrapper)
            )
        )
    }

    @Test
    fun testNullableGeneric() {
        val wrapper = NullableGeneric<Wrapper>(null)

        assertEquals(
            wrapper,
            MessageFormat.decodeFromByteArray(
                NullableGeneric.serializer(Wrapper.serializer()),
                MessageFormat.encodeToByteArray(NullableGeneric.serializer(Wrapper.serializer()), wrapper)
            )
        )
    }

    @Test
    fun testListOfNullableGeneric() {
        val wrapper = ListOfGeneric<Wrapper?>(42, listOf(null, Wrapper(42), null))

        assertEquals(
            wrapper,
            MessageFormat.decodeFromByteArray(
                ListOfGeneric.serializer(Wrapper.serializer().nullable),
                MessageFormat.encodeToByteArray(ListOfGeneric.serializer(Wrapper.serializer().nullable), wrapper)
            )
        )
    }

    @Test
    fun testListOfRecursiveNullableGeneric() {
        val wrapper = ListOfGeneric<Node>(
            42,
            listOf(
                Node(
                    Node(
                        Node(
                            null,
                            3
                        ),
                        2
                    ),
                    1
                )
            )
        )

        assertEquals(
            wrapper,
            MessageFormat.decodeFromByteArray(
                ListOfGeneric.serializer(Node.serializer()),
                MessageFormat.encodeToByteArray(ListOfGeneric.serializer(Node.serializer()), wrapper)
            )
        )
    }

    @Test
    fun testPrimitiveNullable() {
        run {
            val wrapper = PrimitiveNullable("Hello, World")
            assertEquals(
                wrapper,
                MessageFormat.decodeFromByteArray(
                    PrimitiveNullable.serializer(),
                    MessageFormat.encodeToByteArray(PrimitiveNullable.serializer(), wrapper)
                )
            )
        }
       
        run {
            val wrapper = PrimitiveNullable(null)
            assertEquals(
                wrapper,
                MessageFormat.decodeFromByteArray(
                    PrimitiveNullable.serializer(),
                    MessageFormat.encodeToByteArray(PrimitiveNullable.serializer(), wrapper)
                )
            )
        }
    }

    @Test
    fun testEnum() {
        MyEnum.values().forEach { e ->
            val wrapper = EnumTest(e)
            assertEquals(
                wrapper,
                MessageFormat.decodeFromByteArray(
                    EnumTest.serializer(),
                    MessageFormat.encodeToByteArray(EnumTest.serializer(), wrapper)
                )
            )
        }
    }
}
