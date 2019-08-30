package dk.thrane.playground

import kotlin.math.max

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

sealed class Field(val type: FieldType)

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

class BadMessageException(message: String = "") : RuntimeException()

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

abstract class ByteOutStream {
    abstract fun flush()
    abstract fun writeDouble(value: Double)
    abstract fun writeByte(value: Int)
    abstract fun writeByte(value: Byte)

    fun writeInt(v: Int) {
        writeByte(v shr (24) and 0xFF)
        writeByte(v shr (16) and 0xFF)
        writeByte(v shr (8) and 0xFF)
        writeByte(v shr (0) and 0xFF)
    }

    fun writeLong(v: Long) {
        writeByte((v shr 56).toInt())
        writeByte((v shr 48).toInt())
        writeByte((v shr 40).toInt())
        writeByte((v shr 32).toInt())
        writeByte((v shr 24).toInt())
        writeByte((v shr 16).toInt())
        writeByte((v shr 8).toInt())
        writeByte((v shr 0).toInt())
    }

    open fun writeFully(bytes: ByteArray) {
        bytes.forEach { writeByte(it.toInt()) }
    }
}


interface SchemaField<Owner> {
    val idx: Int
}

class StringSchemaField<Owner>(override val idx: Int) : SchemaField<Owner>
class StringSchemaFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class IntSchemaField<Owner>(override val idx: Int) : SchemaField<Owner>
class IntSchemaFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class DoubleSchemaField<Owner>(override val idx: Int) : SchemaField<Owner>
class DoubleSchemaFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class ByteSchemaField<Owner>(override val idx: Int) : SchemaField<Owner>
class ByteSchemaFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class LongSchemaField<Owner>(override val idx: Int) : SchemaField<Owner>
class LongSchemaFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class BooleanSchemaField<Owner>(override val idx: Int) : SchemaField<Owner>
class BooleanSchemaFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class BinarySchemaField<Owner>(override val idx: Int) : SchemaField<Owner>
class BinarySchemaFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>

class ObjectSchemaField<Owner, T : MessageSchema<T>>(override val idx: Int, val schema: T) : SchemaField<Owner>
class ObjectSchemaFieldNullable<Owner, T : MessageSchema<T>>(override val idx: Int, val schema: T) : SchemaField<Owner>

class ListBinaryField<Owner>(override val idx: Int) : SchemaField<Owner>
class ListStringField<Owner>(override val idx: Int) : SchemaField<Owner>
class ListIntField<Owner>(override val idx: Int) : SchemaField<Owner>
class ListDoubleField<Owner>(override val idx: Int) : SchemaField<Owner>
class ListByteField<Owner>(override val idx: Int) : SchemaField<Owner>
class ListLongField<Owner>(override val idx: Int) : SchemaField<Owner>
class ListBooleanField<Owner>(override val idx: Int) : SchemaField<Owner>

class ListStringFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class ListBinaryFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class ListIntFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class ListDoubleFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class ListByteFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class ListLongFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>
class ListBooleanFieldNullable<Owner>(override val idx: Int) : SchemaField<Owner>

class ListObjectField<Owner, T : MessageSchema<T>>(override val idx: Int, val schema: T) : SchemaField<Owner>
class ListObjectFieldNullable<Owner, T : MessageSchema<T>>(override val idx: Int, val schema: T) : SchemaField<Owner>

abstract class MessageSchema<Self> {
    var maxIndex: Int = -1
        private set

    protected fun string(idx: Int) = StringSchemaField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun stringNullable(idx: Int) = StringSchemaFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun binary(idx: Int) = BinarySchemaField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun binaryNullable(idx: Int) = BinarySchemaFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun byte(idx: Int) = ByteSchemaField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun byteNullable(idx: Int) = ByteSchemaFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun int(idx: Int) = IntSchemaField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun intNullable(idx: Int) = IntSchemaFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun long(idx: Int) = LongSchemaField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun longNullable(idx: Int) = LongSchemaFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun double(idx: Int) = DoubleSchemaField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun doubleNullable(idx: Int) = DoubleSchemaFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun boolean(idx: Int) = BooleanSchemaField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun booleanNullable(idx: Int) = BooleanSchemaFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun listByte(idx: Int) = ListByteField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun ListByteNullable(idx: Int) = ListByteFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun listInt(idx: Int) = ListIntField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun ListIntNullable(idx: Int) = ListIntFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun listLong(idx: Int) = ListLongField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun ListLongNullable(idx: Int) = ListLongFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun listDouble(idx: Int) = ListDoubleField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun ListDoubleNullable(idx: Int) = ListDoubleFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun listString(idx: Int) = ListStringField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun ListStringNullable(idx: Int) = ListStringFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun listBoolean(idx: Int) = ListBooleanField<Self>(idx).also { maxIndex = max(idx, maxIndex) }
    protected fun ListBooleanNullable(idx: Int) = ListBooleanFieldNullable<Self>(idx).also { maxIndex = max(idx, maxIndex) }

    protected fun <Obj : MessageSchema<Obj>> obj(idx: Int, schema: Obj) =
        ObjectSchemaField<Self, Obj>(idx, schema).also { maxIndex = max(idx, maxIndex) }

    protected fun <Obj : MessageSchema<Obj>> objNullable(idx: Int, schema: Obj) =
        ObjectSchemaFieldNullable<Self, Obj>(idx, schema).also { maxIndex = max(idx, maxIndex) }

    protected fun <Obj : MessageSchema<Obj>> listObj(idx: Int, schema: Obj) =
        ListObjectField<Self, Obj>(idx, schema).also { maxIndex = max(idx, maxIndex) }

    protected fun <Obj : MessageSchema<Obj>> listObjNullable(idx: Int, schema: Obj) =
        ListObjectFieldNullable<Self, Obj>(idx, schema).also { maxIndex = max(idx, maxIndex) }
}

object TestMessage : MessageSchema<TestMessage>() {
    val text = string(0)
    val nested = objNullable(1, TestMessage)
    val messages = listInt(2)
}

class BoundMessage<T : MessageSchema<T>>(val root: ObjectField) {
    operator fun get(field: StringSchemaField<T>): String {
        return stringFromUtf8(root.getBinary(field.idx))
    }

    operator fun get(field: BinarySchemaField<T>): ByteArray {
        return root.getBinary(field.idx)
    }

    operator fun get(field: ByteSchemaField<T>): Byte {
        return root.getByte(field.idx)
    }

    operator fun get(field: IntSchemaField<T>): Int {
        return root.getInt(field.idx)
    }

    operator fun get(field: LongSchemaField<T>): Long {
        return root.getLong(field.idx)
    }

    operator fun get(field: DoubleSchemaField<T>): Double {
        return root.getDouble(field.idx)
    }

    operator fun get(field: BooleanSchemaField<T>): Boolean {
        return root.getBoolean(field.idx)
    }

    operator fun get(field: StringSchemaFieldNullable<T>): String? {
        return root.getBinaryNullable(field.idx)?.let { stringFromUtf8(it) }
    }

    operator fun get(field: BinarySchemaFieldNullable<T>): ByteArray? {
        return root.getBinaryNullable(field.idx)
    }

    operator fun get(field: ByteSchemaFieldNullable<T>): Byte? {
        return root.getByteNullable(field.idx)
    }

    operator fun get(field: IntSchemaFieldNullable<T>): Int? {
        return root.getIntNullable(field.idx)
    }

    operator fun get(field: LongSchemaFieldNullable<T>): Long? {
        return root.getLongNullable(field.idx)
    }

    operator fun get(field: DoubleSchemaFieldNullable<T>): Double? {
        return root.getDoubleNullable(field.idx)
    }

    operator fun get(field: BooleanSchemaFieldNullable<T>): Boolean? {
        return root.getBooleanNullable(field.idx)
    }

    operator fun get(field: ListIntField<T>): List<Int> {
        return root.getObject(field.idx).fields.map { (it as IntField).value }
    }

    operator fun get(field: ListByteField<T>): List<Byte> {
        return root.getObject(field.idx).fields.map { (it as ByteField).value }
    }

    operator fun get(field: ListLongField<T>): List<Long> {
        return root.getObject(field.idx).fields.map { (it as LongField).value }
    }

    operator fun get(field: ListDoubleField<T>): List<Double> {
        return root.getObject(field.idx).fields.map { (it as DoubleField).value }
    }

    operator fun get(field: ListStringField<T>): List<String> {
        return root.getObject(field.idx).fields.map { stringFromUtf8((it as BinaryField).value) }
    }

    operator fun get(field: ListBinaryField<T>): List<ByteArray> {
        return root.getObject(field.idx).fields.map { (it as BinaryField).value }
    }

    operator fun get(field: ListBooleanField<T>): List<Boolean> {
        return root.getObject(field.idx).fields.map { (it as BooleanField).value }
    }

    operator fun <R : MessageSchema<R>> get(field: ListObjectField<T, R>): List<BoundMessage<R>> {
        return root.getObject(field.idx).fields.map { BoundMessage<R>(it as ObjectField) }
    }

    operator fun get(field: ListIntFieldNullable<T>): List<Int?> {
        return root.getObject(field.idx).fields.map { (it as? IntField)?.value }
    }

    operator fun get(field: ListByteFieldNullable<T>): List<Byte?> {
        return root.getObject(field.idx).fields.map { (it as? ByteField)?.value }
    }

    operator fun get(field: ListLongFieldNullable<T>): List<Long?> {
        return root.getObject(field.idx).fields.map { (it as? LongField)?.value }
    }

    operator fun get(field: ListDoubleFieldNullable<T>): List<Double?> {
        return root.getObject(field.idx).fields.map { (it as? DoubleField)?.value }
    }

    operator fun get(field: ListStringFieldNullable<T>): List<String?> {
        return root.getObject(field.idx).fields.map { (it as? BinaryField)?.value?.let { stringFromUtf8(it) } }
    }

    operator fun get(field: ListBinaryFieldNullable<T>): List<ByteArray?> {
        return root.getObject(field.idx).fields.map { (it as? BinaryField)?.value }
    }

    operator fun get(field: ListBooleanFieldNullable<T>): List<Boolean?> {
        return root.getObject(field.idx).fields.map { (it as? BooleanField)?.value }
    }

    operator fun <R : MessageSchema<R>> get(field: ListObjectFieldNullable<T, R>): List<BoundMessage<R>?> {
        return root.getObject(field.idx).fields.map {
            if (it == NullField) null
            else BoundMessage<R>(it as ObjectField)
        }
    }

    operator fun <R : MessageSchema<R>> get(field: ObjectSchemaField<T, R>): BoundMessage<R> {
        return BoundMessage(root.fields[field.idx] as ObjectField)
    }

    operator fun <R : MessageSchema<R>> get(field: ObjectSchemaFieldNullable<T, R>): BoundMessage<R>? {
        return BoundMessage(root.fields[field.idx] as ObjectField)
    }
}

class BoundOutgoingMessage<T : MessageSchema<T>>(schema: MessageSchema<T>) {
    private val fields = arrayOfNulls<Field>(schema.maxIndex + 1)

    operator fun set(field: StringSchemaField<T>, value: String) {
        fields[field.idx] = BinaryField(value.encodeToUTF8())
    }

    operator fun set(field: StringSchemaFieldNullable<T>, value: String?) {
        fields[field.idx] = if (value != null) BinaryField(value.encodeToUTF8()) else NullField
    }

    operator fun set(field: BinarySchemaField<T>, value: ByteArray) {
        fields[field.idx] = BinaryField(value)
    }

    operator fun set(field: BinarySchemaFieldNullable<T>, value: ByteArray?) {
        fields[field.idx] = if (value != null) BinaryField(value) else NullField
    }

    operator fun set(field: IntSchemaField<T>, value: Int) {
        fields[field.idx] = IntField(value)
    }

    operator fun set(field: IntSchemaFieldNullable<T>, value: Int?) {
        fields[field.idx] = if (value != null) IntField(value) else NullField
    }

    operator fun set(field: ByteSchemaField<T>, value: Byte) {
        fields[field.idx] = ByteField(value)
    }

    operator fun set(field: ByteSchemaFieldNullable<T>, value: Byte?) {
        fields[field.idx] = if (value != null) ByteField(value) else NullField
    }

    operator fun set(field: LongSchemaField<T>, value: Long) {
        fields[field.idx] = LongField(value)
    }

    operator fun set(field: LongSchemaFieldNullable<T>, value: Long?) {
        fields[field.idx] = if (value != null) LongField(value) else NullField
    }

    operator fun set(field: DoubleSchemaField<T>, value: Double) {
        fields[field.idx] = DoubleField(value)
    }

    operator fun set(field: DoubleSchemaFieldNullable<T>, value: Double?) {
        fields[field.idx] = if (value != null) DoubleField(value) else NullField
    }

    operator fun set(field: BooleanSchemaField<T>, value: Boolean) {
        fields[field.idx] = BooleanField(value)
    }

    operator fun set(field: BooleanSchemaFieldNullable<T>, value: Boolean?) {
        fields[field.idx] = if (value != null) BooleanField(value) else NullField
    }

    operator fun set(field: ListByteField<T>, value: List<Byte>) {
        fields[field.idx] = ObjectField(value.map { ByteField(it) })
    }

    operator fun set(field: ListIntField<T>, value: List<Int>) {
        fields[field.idx] = ObjectField(value.map { IntField(it) })
    }

    operator fun set(field: ListLongField<T>, value: List<Long>) {
        fields[field.idx] = ObjectField(value.map { LongField(it) })
    }

    operator fun set(field: ListDoubleField<T>, value: List<Double>) {
        fields[field.idx] = ObjectField(value.map { DoubleField(it) })
    }

    operator fun set(field: ListStringField<T>, value: List<String>) {
        fields[field.idx] = ObjectField(value.map { BinaryField(it.encodeToUTF8()) })
    }

    operator fun set(field: ListBinaryField<T>, value: List<ByteArray>) {
        fields[field.idx] = ObjectField(value.map { BinaryField(it) })
    }

    operator fun set(field: ListBooleanField<T>, value: List<Boolean>) {
        fields[field.idx] = ObjectField(value.map { BooleanField(it) })
    }

    operator fun <R : MessageSchema<R>> set(field: ListObjectField<T, R>, value: List<BoundOutgoingMessage<R>>) {
        fields[field.idx] = ObjectField(value.map { it.build() })
    }

    operator fun set(field: ListByteFieldNullable<T>, value: List<Byte?>) {
        fields[field.idx] = ObjectField(value.map { if (it == null) NullField else ByteField(it) })
    }

    operator fun set(field: ListIntFieldNullable<T>, value: List<Int?>) {
        fields[field.idx] = ObjectField(value.map { if (it == null) NullField else IntField(it) })
    }

    operator fun set(field: ListLongFieldNullable<T>, value: List<Long?>) {
        fields[field.idx] = ObjectField(value.map { if (it == null) NullField else LongField(it) })
    }

    operator fun set(field: ListDoubleFieldNullable<T>, value: List<Double?>) {
        fields[field.idx] = ObjectField(value.map { if (it == null) NullField else DoubleField(it) })
    }

    operator fun set(field: ListStringFieldNullable<T>, value: List<String?>) {
        fields[field.idx] = ObjectField(value.map { if (it == null) NullField else BinaryField(it.encodeToUTF8()) })
    }

    operator fun set(field: ListBinaryFieldNullable<T>, value: List<ByteArray?>) {
        fields[field.idx] = ObjectField(value.map { if (it == null) NullField else BinaryField(it) })
    }

    operator fun set(field: ListBooleanFieldNullable<T>, value: List<Boolean?>) {
        fields[field.idx] = ObjectField(value.map { if (it == null) NullField else BooleanField(it) })
    }

    operator fun <R : MessageSchema<R>> set(
        field: ListObjectFieldNullable<T, R>,
        value: List<BoundOutgoingMessage<R>?>
    ) {
        fields[field.idx] = ObjectField(value.map { it?.build() ?: NullField })
    }


    operator fun <R : MessageSchema<R>> set(
        field: ObjectSchemaField<T, R>,
        builder: (BoundOutgoingMessage<R>) -> Unit
    ) {
        fields[field.idx] = BoundOutgoingMessage(field.schema).also(builder).build()
    }

    operator fun <R : MessageSchema<R>> set(
        field: ObjectSchemaField<T, R>,
        value: BoundOutgoingMessage<R>
    ) {
        fields[field.idx] = value.build()
    }

    operator fun <R : MessageSchema<R>> set(
        field: ObjectSchemaFieldNullable<T, R>,
        builder: ((BoundOutgoingMessage<R>) -> Unit)?
    ) {
        fields[field.idx] = if (builder != null) BoundOutgoingMessage(field.schema).also(builder).build() else NullField
    }

    operator fun <R : MessageSchema<R>> set(
        field: ObjectSchemaFieldNullable<T, R>,
        value: (BoundOutgoingMessage<R>)?
    ) {
        fields[field.idx] = if (value != null) value.build() else NullField
    }

    fun build(): ObjectField {
        return ObjectField(fields.map { it ?: NullField })
    }
}

fun parseMessage(buffer: ByteStream): Field {
    return when (FieldType.valueOf(buffer.read()) ?: throw IllegalStateException()) {
        FieldType.BYTE -> ByteField(buffer.read().toByte())
        FieldType.INT -> IntField(buffer.readInt())
        FieldType.LONG -> LongField(buffer.readLong())
        FieldType.DOUBLE -> DoubleField(buffer.readDouble())
        FieldType.BOOLEAN -> BooleanField(buffer.read() != 0)
        FieldType.OBJ_END -> ObjectEndIndicator
        FieldType.NULL -> NullField

        FieldType.BINARY -> {
            val length = buffer.readInt()
            val destinationBuffer = ByteArray(length)
            buffer.readFully(destinationBuffer)
            BinaryField(destinationBuffer)
        }

        FieldType.OBJ_START -> {
            val fields = ArrayList<Field>()
            while (true) {
                val element = parseMessage(buffer)
                if (element == ObjectEndIndicator) break
                fields.add(element)
            }

            ObjectField(fields)
        }
    }
}

fun writeMessage(out: ByteOutStream, field: Field) {
    out.writeByte(field.type.ordinal)
    when (field) {
        is ByteField -> out.writeByte(field.value)
        is IntField -> out.writeInt(field.value)
        is LongField -> out.writeLong(field.value)
        is DoubleField -> out.writeDouble(field.value)
        is BooleanField -> out.writeByte(if (field.value) 1 else 0)
        ObjectEndIndicator -> {
        }
        NullField -> {
        }

        is BinaryField -> {
            val payload = field.value
            out.writeInt(payload.size)
            out.writeFully(payload)
        }

        is ObjectField -> {
            field.fields.forEach { child ->
                writeMessage(out, child)
            }

            writeMessage(out, ObjectEndIndicator)
        }
    }

    out.flush()
}

inline fun <T : MessageSchema<T>> buildOutgoing(
    schema: T,
    builder: (BoundOutgoingMessage<T>) -> Unit
): BoundOutgoingMessage<T> {
    return BoundOutgoingMessage(schema).also(builder)
}

