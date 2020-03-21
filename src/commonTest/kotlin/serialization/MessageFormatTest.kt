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

class MessageFormatTest {
    @Test
    fun testSimpleWrapper() {
        val wrapper = Wrapper(42)
        assertEquals(
            wrapper,
            MessageFormat.load(
                Wrapper.serializer(),
                MessageFormat.dump(Wrapper.serializer(), wrapper)
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
            MessageFormat.load(
                Node.serializer(),
                MessageFormat.dump(Node.serializer(), wrapper)
            )
        )
    }

    @Test
    fun testGeneric() {
        val wrapper = Generic(Wrapper(42))

        assertEquals(
            wrapper,
            MessageFormat.load(
                Generic.serializer(Wrapper.serializer()),
                MessageFormat.dump(Generic.serializer(Wrapper.serializer()), wrapper)
            )
        )
    }

    @Test
    fun testListOfGeneric() {
        val wrapper = ListOfGeneric(1337, listOf(Wrapper(42)))

        assertEquals(
            wrapper,
            MessageFormat.load(
                ListOfGeneric.serializer(Wrapper.serializer()),
                MessageFormat.dump(ListOfGeneric.serializer(Wrapper.serializer()), wrapper)
            )
        )
    }

    @Test
    fun testNullableGeneric() {
        val wrapper = NullableGeneric<Wrapper>(null)

        assertEquals(
            wrapper,
            MessageFormat.load(
                NullableGeneric.serializer(Wrapper.serializer()),
                MessageFormat.dump(NullableGeneric.serializer(Wrapper.serializer()), wrapper)
            )
        )
    }

    @Test
    fun testListOfNullableGeneric() {
        val wrapper = ListOfGeneric<Wrapper?>(42, listOf(null, Wrapper(42), null))

        assertEquals(
            wrapper,
            MessageFormat.load(
                ListOfGeneric.serializer(Wrapper.serializer().nullable),
                MessageFormat.dump(ListOfGeneric.serializer(Wrapper.serializer().nullable), wrapper)
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
            MessageFormat.load(
                ListOfGeneric.serializer(Node.serializer()),
                MessageFormat.dump(ListOfGeneric.serializer(Node.serializer()), wrapper)
            )
        )
    }
}
