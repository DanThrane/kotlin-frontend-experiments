package dk.thrane.playground.psql

import dk.thrane.playground.serialization.OutputBuffer
import dk.thrane.playground.serialization.writeFully
import dk.thrane.playground.serialization.writeInt
import dk.thrane.playground.serialization.writeShort

sealed class FrontendMessage(val type: Byte?, val writeLength: Boolean = true) {
    protected abstract fun serializePayload(out: OutputBuffer)

    fun serialize(out: OutputBuffer) {
        if (type != null) out.writeByte(type)
        val initialPtr = out.ptr
        if (writeLength) {
            out.ptr += 4 // Make room for length
        }

        require(out.ptr < out.array.size) { "Not enough room for message" }
        serializePayload(out)

        if (writeLength) {
            val endPtr = out.ptr
            out.ptr = initialPtr
            out.writeInt(endPtr - initialPtr)
            out.ptr = endPtr
        }
    }

    class Bind(
        val destinationPortal: String,
        val sourcePreparedStatement: String,
        val formatCodes: ShortArray,
        val parameterValues: Array<ByteArray?>,
        val resultColumnFormatCodes: ShortArray
    ) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            writeString(out, destinationPortal)
            writeString(out, sourcePreparedStatement)

            out.writeShort(formatCodes.size.toShort())
            formatCodes.forEach { out.writeShort(it) }

            out.writeShort(parameterValues.size.toShort())
            parameterValues.forEach { value ->
                if (value == null) {
                    out.writeInt(-1)
                } else {
                    out.writeInt(value.size)
                    out.writeFully(value)
                }
            }

            out.writeShort(resultColumnFormatCodes.size.toShort())
            resultColumnFormatCodes.forEach { out.writeShort(it) }
        }

        companion object {
            const val Type = 'B'.toByte()
        }
    }

    enum class CloseTarget(val type: Byte) {
        PREPARED_STATEMENT('S'.toByte()),
        PORTAL('P'.toByte())
    }

    class Close(val targetType: CloseTarget, val target: String) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            out.writeByte(targetType.type)
            writeString(out, target)
        }

        companion object {
            const val Type = 'C'.toByte()
        }
    }

    class CopyData(val data: ByteArray) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            out.writeFully(data)
        }

        companion object {
            const val Type = 'd'.toByte()
        }
    }

    object CopyDone : FrontendMessage('c'.toByte()) {
        override fun serializePayload(out: OutputBuffer) {

        }
    }

    class CopyFail(val reason: String) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            writeString(out, reason)
        }

        companion object {
            const val Type = 'f'.toByte()
        }
    }

    class Describe(val targetType: CloseTarget, val target: String) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            out.writeByte(targetType.type)
            writeString(out, target)
        }

        companion object {
            const val Type = 'D'.toByte()
        }
    }

    class Execute(val portalTarget: String, val maxRows: Int) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            writeString(out, portalTarget)
            out.writeInt(maxRows)
        }

        companion object {
            const val Type = 'E'.toByte()
        }
    }

    object Flush : FrontendMessage('H'.toByte()) {
        override fun serializePayload(out: OutputBuffer) {

        }
    }

    sealed class FunctionCallArgument {
        object Null : FunctionCallArgument()
        class Data(val type: Int, val bytes: ByteArray) : FunctionCallArgument()
    }

    class FunctionCall(
        val numberOfArguments: Short,
        val argumentFormatCodes: ShortArray,
        val arguments: Array<FunctionCallArgument>,
        val resultFormatCode: Short
    ) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            out.writeShort(numberOfArguments)
            argumentFormatCodes.forEach { out.writeShort(it) }
            arguments.forEach { arg ->
                when (arg) {
                    FunctionCallArgument.Null -> {
                        out.writeInt(-1)
                    }

                    is FunctionCallArgument.Data -> {
                        out.writeInt(arg.type)
                        out.writeFully(arg.bytes)
                    }
                }
            }
            out.writeShort(resultFormatCode)
        }

        companion object {
            const val Type = 'F'.toByte()
        }
    }

    class Parse(
        val destinationStatement: String,
        val queryString: String,
        val prespecifiedTypes: IntArray
    ) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            writeString(out, destinationStatement)
            writeString(out, queryString)
            out.writeShort(prespecifiedTypes.size.toShort())
            prespecifiedTypes.forEach { out.writeInt(it) }
        }

        companion object {
            const val Type = 'P'.toByte()
        }
    }

    class Password(val password: String) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            writeString(out, password)
        }

        companion object {
            const val Type = 'p'.toByte()
        }
    }

    class Query(val query: String) : FrontendMessage(Type) {
        override fun serializePayload(out: OutputBuffer) {
            writeString(out, query)
        }

        companion object {
            const val Type = 'Q'.toByte()
        }
    }

    class StartupMessage(val parameters: List<Pair<String, String>>) : FrontendMessage(null) {
        override fun serializePayload(out: OutputBuffer) {
            out.writeShort(3)
            out.writeShort(0)
            parameters.forEach { (name, value) ->
                writeString(out, name)
                writeString(out, value)
            }
            out.writeByte(0)
        }
    }

    object Sync : FrontendMessage('S'.toByte()) {
        override fun serializePayload(out: OutputBuffer) {
            // Do nothing
        }
    }

    object Terminate : FrontendMessage('X'.toByte()) {
        override fun serializePayload(out: OutputBuffer) {
            // Do nothing
        }
    }

    companion object {
        fun writeString(out: OutputBuffer, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            out.writeFully(bytes)
            out.writeByte(0)
        }
    }
}
