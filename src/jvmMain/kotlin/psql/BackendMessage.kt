package dk.thrane.playground.psql

import dk.thrane.playground.Log
import dk.thrane.playground.io.AsyncByteInStream
import dk.thrane.playground.io.readByte
import dk.thrane.playground.io.readFully
import dk.thrane.playground.io.readInt
import dk.thrane.playground.serialization.InputBuffer
import dk.thrane.playground.serialization.readFully
import dk.thrane.playground.serialization.readInt
import dk.thrane.playground.serialization.readShort
import kotlinx.io.IOException

sealed class BackendMessage(val type: Byte) {
    sealed class Authentication(val subType: Int) : BackendMessage(Type) {
        object Ok : Authentication(T_OK)
        object ClearTextPassword : Authentication(T_ClearTextPassword)
        class MD5Password(val salt: ByteArray) : Authentication(T_MD5Password)

        companion object {
            const val Type = 'R'.toByte()

            const val T_OK = 0
            const val T_KerberosV5 = 2
            const val T_ClearTextPassword = 3
            const val T_MD5Password = 5
            const val T_SCMCCredential = 6
            const val T_GSS = 7
            const val T_SSPI = 8
            const val T_SASL = 10
            const val T_SASLContinue = 11
            const val T_SASLFinal = 12
        }
    }

    object CloseComplete : BackendMessage('3'.toByte())

    data class CommandComplete(val commandTag: String) : BackendMessage(Type) {
        companion object {
            const val Type = 'C'.toByte()
        }
    }

    data class CopyData(val dataStream: ByteArray) : BackendMessage(Type) {
        companion object {
            const val Type = 'd'.toByte()
        }
    }

    enum class CopyResponseTypes(val type: Byte) {
        IN('G'.toByte()),
        OUT('H'.toByte()),
        BOTH('W'.toByte())
    }

    data class CopyResponse(
        val responseType: CopyResponseTypes,
        val format: Byte,
        val columnFormats: ShortArray
    ) : BackendMessage(responseType.type)

    sealed class Column {
        object Null : Column()
        data class Data(val bytes: ByteArray) : Column() {
            override fun toString(): String = "Column.Data(${bytes.toList()})"
        }
    }

    data class DataRow(val columns: Array<Column>) : BackendMessage(Type) {
        companion object {
            const val Type = 'D'.toByte()
        }
    }

    object CopyDone : BackendMessage('c'.toByte())

    object EmptyQueryResponse : BackendMessage('I'.toByte())

    data class FunctionCallResponse(val column: Column) : BackendMessage(Type) {
        companion object {
            const val Type = 'V'.toByte()
        }
    }

    data class NegotiateProtocolVersion(
        val suggestedVersion: Int,
        val unknownProtocolVersions: Array<String>
    ) : BackendMessage(Type) {
        companion object {
            const val Type = 'v'.toByte()
        }
    }

    object NoData : BackendMessage('n'.toByte())

    data class NoticeResponse(val fields: List<Field>) : BackendMessage(Type) {
        class Field(val type: Byte, val value: String)

        companion object {
            const val Type = 'N'.toByte()
        }
    }

    // Skipped a few which didn't seem relevant

    object ParseComplete : BackendMessage('1'.toByte())

    object PortalSuspended : BackendMessage('s'.toByte())

    enum class ReadyForQuerySignal(val type: Byte) {
        IDLE('I'.toByte()),
        TRANSACTION('T'.toByte()),
        ERROR('E'.toByte())
    }

    data class ReadyForQuery(val signal: ReadyForQuerySignal) : BackendMessage(Type) {
        companion object {
            const val Type = 'Z'.toByte()
        }
    }

    data class RowDescription(val fields: Array<Field>) : BackendMessage(Type) {
        class Field(
            val name: String,
            val objectId: Int?,
            val column: Short?,
            val typeObjectId: Int,
            val dataTypeSize: Short,
            val typeModifier: Int,
            val formatCode: Short
        )

        companion object {
            const val Type = 'T'.toByte()
        }
    }

    data class ParameterStatus(val parameterName: String, val value: String) : BackendMessage(Type) {
        companion object {
            const val Type = 'S'.toByte()
        }
    }

    data class BackendKeyData(val processId: Int, val secretKey: Int) : BackendMessage(Type) {
        companion object {
            const val Type = 'K'.toByte()
        }
    }

    data class ErrorResponse(val fields: List<Pair<Byte, String>>) : BackendMessage(Type) {
        companion object {
            const val Type = 'E'.toByte()
        }
    }

    companion object {
        private val log = Log("PSQLMessage")
        private val allowedPacketSize = 0..(1024 * 1024 * 8)

        suspend fun readMessage(ins: AsyncByteInStream): BackendMessage? {
            val type = ins.readByte()
            val length = ins.readInt() - 4
            if (length !in allowedPacketSize) throw IOException("Packet too large: $length.")
            val payload = ByteArray(length)
            ins.readFully(payload)
            val inputBuffer = InputBuffer(payload)

            return when (type) {
                Authentication.Type -> {
                    return when (val subType = inputBuffer.readInt()) {
                        Authentication.T_OK -> Authentication.Ok
                        Authentication.T_ClearTextPassword -> Authentication.ClearTextPassword
                        Authentication.T_MD5Password -> Authentication.MD5Password(
                            ByteArray(4).also { inputBuffer.readFully(it) }
                        )
                        else -> throw NotImplementedError("Unimplemented authentication message: $subType")
                    }
                }

                CloseComplete.type -> CloseComplete

                CommandComplete.Type -> CommandComplete(readString(inputBuffer))

                CopyData.Type -> CopyData(payload)

                CopyDone.type -> CopyDone

                CopyResponseTypes.IN.type, CopyResponseTypes.BOTH.type, CopyResponseTypes.OUT.type -> {
                    val format = inputBuffer.read().toByte()
                    val numberOfColumns = inputBuffer.readShort()
                    if (numberOfColumns < 0) throw IOException("Bad packet")

                    val columnFormats = ShortArray(numberOfColumns.toInt())
                    for (i in columnFormats.indices) {
                        columnFormats[i] = inputBuffer.readShort()
                    }

                    CopyResponse(CopyResponseTypes.values().find { it.type == type }!!, format, columnFormats)
                }

                DataRow.Type -> {
                    val numberOfColumns = inputBuffer.readShort()
                    if (numberOfColumns < 0) throw IOException("Bad packet")

                    DataRow(
                        Array(numberOfColumns.toInt()) { readColumn(inputBuffer) }
                    )
                }

                EmptyQueryResponse.type -> EmptyQueryResponse

                FunctionCallResponse.Type -> {
                    FunctionCallResponse(readColumn(inputBuffer))
                }

                NegotiateProtocolVersion.Type -> {
                    val suggestedVarargs = inputBuffer.readInt()
                    val numberOfUnknownOptions = inputBuffer.readInt()
                    if (numberOfUnknownOptions < 0) throw IOException("Bad packet")
                    val unknownOptions = Array(numberOfUnknownOptions) { readString(inputBuffer) }

                    NegotiateProtocolVersion(suggestedVarargs, unknownOptions)
                }

                NoData.type -> NoData

                NoticeResponse.Type -> {
                    val fields = ArrayList<NoticeResponse.Field>()
                    while (true) {
                        val fieldType = inputBuffer.read().toByte()
                        if (fieldType == 0.toByte()) break
                        val value = readString(inputBuffer)
                        fields.add(NoticeResponse.Field(fieldType, value))
                    }

                    NoticeResponse(fields)
                }

                ParseComplete.type -> ParseComplete

                PortalSuspended.type -> PortalSuspended

                ReadyForQuery.Type -> {
                    val rawSignal = inputBuffer.read().toByte()
                    val signal = ReadyForQuerySignal.values().find { it.type == rawSignal }
                        ?: throw IOException("Bad ReadyForQuery signal: $rawSignal")

                    ReadyForQuery(signal)
                }

                RowDescription.Type -> {
                    val numberOfFields = inputBuffer.readShort()
                    if (numberOfFields < 0) throw IOException("Bad packet")

                    RowDescription(
                        Array(numberOfFields.toInt()) {
                            RowDescription.Field(
                                readString(inputBuffer),
                                inputBuffer.readInt().takeIf { it != 0 },
                                inputBuffer.readShort().takeIf { it != 0.toShort() },
                                inputBuffer.readInt(),
                                inputBuffer.readShort(), // Negative values are allowed
                                inputBuffer.readInt(),
                                inputBuffer.readShort()
                            )
                        }
                    )
                }

                ParameterStatus.Type -> {
                    ParameterStatus(readString(inputBuffer), readString(inputBuffer))
                }

                BackendKeyData.Type -> {
                    BackendKeyData(inputBuffer.readInt(), inputBuffer.readInt())
                }

                ErrorResponse.Type -> {
                    val fields = ArrayList<Pair<Byte, String>>()
                    while (true) {
                        val fieldType = inputBuffer.read().toByte()
                        if (fieldType == 0.toByte()) break
                        val message = readString(inputBuffer)
                        fields.add(Pair(fieldType, message))
                    }

                    ErrorResponse(fields)
                }

                50.toByte() -> null

                else -> {
                    log.warn("Unimplemented message type: $type (${type.toChar()})")
                    null
                }
            }
        }

        private fun readColumn(inputBuffer: InputBuffer): Column {
            val colLength = inputBuffer.readInt()
            if (colLength <= -2) throw IOException("Bad packet")

            return if (colLength == -1) {
                Column.Null
            } else {
                val bytes = ByteArray(colLength)
                inputBuffer.readFully(bytes)
                Column.Data(bytes)
            }
        }

        private fun readString(inputBuffer: InputBuffer): String {
            var ptr = inputBuffer.ptr
            val array = inputBuffer.array
            val size = array.size
            while (ptr < size) {
                if (array[ptr] == 0.toByte()) {
                    break
                }

                ptr++
            }

            val bytesToCopy = ptr - inputBuffer.ptr
            val stringBuffer = ByteArray(bytesToCopy)
            array.copyInto(stringBuffer, startIndex = inputBuffer.ptr, endIndex = ptr)
            inputBuffer.ptr = ptr + 1
            return String(stringBuffer, Charsets.UTF_8)
        }
    }
}
