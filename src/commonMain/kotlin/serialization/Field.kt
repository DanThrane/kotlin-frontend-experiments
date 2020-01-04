package dk.thrane.playground.serialization

import dk.thrane.playground.RPCException
import dk.thrane.playground.ResponseCode

enum class FieldType {
    BYTE,
    INT,
    LONG,
    DOUBLE,
    BINARY,
    BOOLEAN,
    OBJ_START,
    OBJ_END,
    NULL;

    companion object {
        fun valueOf(type: Int): FieldType? = values().getOrNull(type)
    }
}

// Each message is simply a field
// An object (initiated by OBJ_START) will read fields until OBJ_END is reached
// We can always determine the end of an object
// We assume that all messages can be kept in memory (send multiple for large messages)

sealed class Field(val type: FieldType) {
    companion object
}

class ByteField(val value: Byte) : Field(FieldType.BYTE)
class IntField(val value: Int) : Field(FieldType.INT)
class LongField(val value: Long) : Field(FieldType.LONG)
class DoubleField(val value: Double) : Field(FieldType.DOUBLE)
class BinaryField(val value: ByteArray) : Field(FieldType.BINARY)
class BooleanField(val value: Boolean) : Field(FieldType.BOOLEAN)
object ObjectEndIndicator : Field(FieldType.OBJ_END)
object NullField : Field(FieldType.NULL)

class ObjectField(val fields: List<Field>) : Field(FieldType.OBJ_START) {
    fun getByte(idx: Int): Byte {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type != FieldType.BYTE) throw BadMessageException()
        return (field as ByteField).value
    }

    fun getByteNullable(idx: Int): Byte? {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type == FieldType.NULL) return null
        if (field.type != FieldType.BYTE) throw BadMessageException()
        return (field as ByteField).value
    }

    fun getInt(idx: Int): Int {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type != FieldType.INT) throw BadMessageException()
        return (field as IntField).value
    }

    fun getIntNullable(idx: Int): Int? {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type == FieldType.NULL) return null
        if (field.type != FieldType.INT) throw BadMessageException()
        return (field as IntField).value
    }

    fun getLong(idx: Int): Long {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type != FieldType.LONG) throw BadMessageException()
        return (field as LongField).value
    }

    fun getLongNullable(idx: Int): Long? {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type == FieldType.NULL) return null
        if (field.type != FieldType.LONG) throw BadMessageException()
        return (field as LongField).value
    }

    fun getDouble(idx: Int): Double {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type != FieldType.DOUBLE) throw BadMessageException()
        return (field as DoubleField).value
    }

    fun getDoubleNullable(idx: Int): Double? {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type == FieldType.NULL) return null
        if (field.type != FieldType.DOUBLE) throw BadMessageException()
        return (field as DoubleField).value
    }

    fun getBinary(idx: Int): ByteArray {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type != FieldType.BINARY) throw BadMessageException()
        return (field as BinaryField).value
    }

    fun getBinaryNullable(idx: Int): ByteArray? {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type == FieldType.NULL) return null
        if (field.type != FieldType.BINARY) throw BadMessageException()
        return (field as BinaryField).value
    }

    fun getBoolean(idx: Int): Boolean {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type != FieldType.BOOLEAN) throw BadMessageException()
        return (field as BooleanField).value
    }

    fun getBooleanNullable(idx: Int): Boolean? {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type == FieldType.NULL) return null
        if (field.type != FieldType.BOOLEAN) throw BadMessageException()
        return (field as BooleanField).value
    }

    fun getObject(idx: Int): ObjectField {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type != FieldType.OBJ_START) throw BadMessageException()
        return (field as ObjectField)
    }

    fun getObjectNullable(idx: Int): ObjectField? {
        if (idx !in fields.indices) throw BadMessageException()
        val field = fields[idx]
        if (field.type == FieldType.NULL) return null
        if (field.type != FieldType.OBJ_START) throw BadMessageException()
        return (field as ObjectField)
    }
}

class BadMessageException(message: String = "") : RPCException(ResponseCode.BAD_REQUEST, message)
