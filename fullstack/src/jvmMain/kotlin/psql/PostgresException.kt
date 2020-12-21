package dk.thrane.playground.psql

sealed class PostgresException : RuntimeException() {
    class Parsing(val fields: List<Pair<Byte, String>>) : PostgresException() {
        override val message: String = "Parsing failed! $fields"
    }

    class Generic(val fields: List<Pair<Byte, String>>) : PostgresException() {
        override val message: String = "$fields"
    }
}
