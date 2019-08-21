package dk.thrane.playground

enum class FieldType {
    BYTE,
    INT,
    LONG,
    DOUBLE,
    STRING,
    BOOLEAN,
    OBJ_START,
    OBJ_END;

    companion object {
        fun valueOf(type: Int): FieldType? = values().getOrNull(type)
    }
}

// Each message is simply a field
// An object (initiated by OBJ_START) will read fields until OBJ_END is reached
// We can always determine the end of an object
// We assume that all messages can be kept in memory (send multiple for large messages)

sealed class Field(val type: FieldType)

class ByteField(val value: Byte) : Field(FieldType.BYTE)
class IntField(val value: Int) : Field(FieldType.INT)
class LongField(val value: Long) : Field(FieldType.LONG)
class DoubleField(val value: Double) : Field(FieldType.DOUBLE)
class StringField(val value: String) : Field(FieldType.STRING)
class BooleanField(val value: Boolean) : Field(FieldType.BOOLEAN)
class ObjectField(val fields: List<Field>) : Field(FieldType.OBJ_START)
object ObjectEndIndicator : Field(FieldType.OBJ_END)

abstract class ByteStream(private val buffer: ByteArray) {
    private var ptr = 0

    fun read(): Int = buffer[ptr++].toInt()

    fun readInt(): Int {
        val ch1 = read()
        val ch2 = read()
        val ch3 = read()
        val ch4 = read()

        if (ch1 or ch2 or ch3 or ch4 < 0) {
            throw IllegalStateException("EOF")
        }

        return (ch1 shl 24) + (ch2 shl 16) + (ch3 shl 8) + (ch4 shl 0)
    }

    fun readLong(): Long {
        val result = (buffer[0].toLong() shl 56) +
                ((buffer[1].toLong() and 255) shl 48) +
                ((buffer[2].toLong() and 255) shl 40) +
                ((buffer[3].toLong() and 255) shl 32) +
                ((buffer[4].toLong() and 255) shl 24) +
                (buffer[5].toLong() and 255 shl 16) +
                (buffer[6].toLong() and 255 shl 8) +
                (buffer[7].toLong() and 255 shl 0)
        ptr += 8
        return result
    }

    abstract fun readDouble(): Double

    fun readFully(destination: ByteArray) {
        if (destination.size + ptr > buffer.size) throw IllegalStateException("EOF")
        repeat(destination.size) { idx ->
            destination[idx] = buffer[ptr++]
        }
    }
}

fun parse(buffer: ByteStream): Field {
    return when (FieldType.valueOf(buffer.read()) ?: throw IllegalStateException()) {
        FieldType.BYTE -> ByteField(buffer.read().toByte())
        FieldType.INT -> IntField(buffer.readInt())
        FieldType.LONG -> LongField(buffer.readLong())
        FieldType.DOUBLE -> DoubleField(buffer.readDouble())
        FieldType.BOOLEAN -> BooleanField(buffer.read() != 0)
        FieldType.OBJ_END -> ObjectEndIndicator

        FieldType.STRING -> {
            val length = buffer.readInt()
            val destinationBuffer = ByteArray(length)
            buffer.readFully(destinationBuffer)
            StringField(String(destinationBuffer, Charsets.UTF_8))
        }

        FieldType.OBJ_START -> {
            val fields = ArrayList<Field>()
            while (true) {
                val element = parse(buffer)
                if (element == ObjectEndIndicator) break
                fields.add(element)
            }

            ObjectField(fields)
        }
    }
}
