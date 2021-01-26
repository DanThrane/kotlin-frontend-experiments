package dk.thrane.playground.psql

import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Provides an enhanced prepared statement adding support for named parameters.
 *
 * Named parameters use the following syntax: "?PARAMNAME".
 */
@OptIn(ExperimentalSerializationApi::class)
class PreparedStatement<Input>(
    statement: String,
    private val inSerializer: KSerializer<Input>,
) {
    private val parameterNamesToIndex: Map<String, List<Int>>
    private val indexToParameterName: List<String>
    private val preparedStatement: String
    private val header: List<PGType<*>>
    private var statementIds = WeakHashMap<PostgresConnection, PreparedStatementId>()

    init {
        val types = inSerializer.descriptor.elementDescriptors
            .mapIndexed { index, elem ->
                inSerializer.descriptor.getElementName(index) to when (elem.kind) {
                    PrimitiveKind.INT -> PGType.Int4
                    PrimitiveKind.BOOLEAN -> PGType.Bool
                    PrimitiveKind.BYTE -> TODO()
                    PrimitiveKind.SHORT -> PGType.Int2
                    PrimitiveKind.LONG -> PGType.Int8
                    PrimitiveKind.FLOAT -> PGType.Float4
                    PrimitiveKind.DOUBLE -> PGType.Float8
                    PrimitiveKind.CHAR -> PGType.Char
                    PrimitiveKind.STRING -> PGType.Text
                    StructureKind.CLASS -> TODO()
                    StructureKind.LIST -> {
                        if (elem.elementDescriptors.single().kind == PrimitiveKind.BYTE) {
                            PGType.Bytea
                        } else {
                            TODO()
                        }
                    }
                    StructureKind.MAP -> TODO()
                    StructureKind.OBJECT -> TODO()
                    SerialKind.ENUM-> PGType.Text
                    PolymorphicKind.SEALED -> TODO()
                    PolymorphicKind.OPEN -> TODO()
                    else -> throw IllegalStateException("Unhandled type: ${elem.kind}")
                }
            }
            .toMap()

        val parameterNamesToIndex = HashMap<String, List<Int>>()

        val indexToParameterName = ArrayList<String>()

        val queryBuilder = StringBuilder()
        var parameterIndex = 0
        var stringIndex = 0
        while (stringIndex < statement.length) {
            // Find the next parameter by looking for a '?'
            val nextParameter = statement.indexOf('?', stringIndex)
            if (nextParameter == -1) {
                // We're at the end of the string. We just append the remainder to the query.
                queryBuilder.append(statement.substring(stringIndex))
                break
            }

            // Add everything up to and including the '?'. We use this for the prepared statement.
            queryBuilder.append(statement.substring(stringIndex, nextParameter)) // skip '?'
            queryBuilder.append("\$${++parameterIndex}")

            // Parse the parameter name. We only allow alphanumeric and underscores.
            val endOfParameterName = statement.substring(nextParameter + 1)
                .indexOfFirst { it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' && it != '_' }
                .takeIf { it != -1 }
                ?.let { it + nextParameter + 1 }
                ?: statement.length

            // Write down the parameter name and move past it
            val parameterName = statement.substring(nextParameter + 1, endOfParameterName)
            stringIndex = endOfParameterName

            indexToParameterName.add(parameterName)
            parameterNamesToIndex[parameterName] =
                (parameterNamesToIndex[parameterName] ?: emptyList()) + listOf(parameterIndex - 1)
        }

        this.parameterNamesToIndex = parameterNamesToIndex
        preparedStatement = queryBuilder.toString()
        header = indexToParameterName.map { types[it] ?: throw IllegalArgumentException("Unknown parameter: '$it'") }
        this.indexToParameterName = indexToParameterName
    }

    suspend fun prepareForConnection(conn: PostgresConnection): PreparedStatementId {
        return conn.createNativePreparedStatement(preparedStatement, header)
    }

    fun query(conn: PostgresConnection, rows: Flow<Input>): Flow<DBRow> {
        return flow {
            val statementId = statementIds[conn]
                ?: prepareForConnection(conn).also { statementIds[conn] = it }

            emitAll(
                conn.invokePreparedStatement(
                    statementId,
                    rows.map {
                        val target = arrayOfNulls<Any>(header.size)
                        PostgresRootEncoder(target, header, parameterNamesToIndex)
                            .encodeSerializableValue(inSerializer, it)
                        target.toList()
                    }
                )
            )
        }
    }
}

suspend fun <I> PreparedStatement<I>.command(conn: PostgresConnection, row: I) {
    query(conn, flowOf(row)).collect()
}
