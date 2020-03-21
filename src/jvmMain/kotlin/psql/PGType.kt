package dk.thrane.playground.psql

import java.math.BigDecimal

sealed class PGType<KtType>(val typeName: String) {
    override fun toString() = typeName

    object Int2 : PGType<Short>("int2")
    object Int4 : PGType<Int>("int4")
    object Int8 : PGType<Long>("int8")
    object Varchar : PGType<String>("varchar")
    object Text : PGType<String>("text")
    object Json : PGType<String>("json")
    object Jsonb : PGType<String>("jsonb")
    object Void : PGType<Unit>("void")
    object Xml : PGType<String>("xml")
    object Bool : PGType<Boolean>("bool")
    object Float4 : PGType<Float>("float4")
    object Float8 : PGType<Double>("float8")
    object Numeric : PGType<BigDecimal>("numeric")
    object Char : PGType<kotlin.Char>("char")
    object Bytea : PGType<ByteArray>("bytea")

    /*
        const val money = "money"
        const val date = "date"
        const val time = "time"
        const val timestamp = "timestamp"
        const val void = "void"
        const val uuid = "uuid"
        const val any = "any"
     */

    class Unknown(typeName: String) : PGType<String>(typeName) {
        override fun toString(): String = "Unknown($typeName)"
    }
}
