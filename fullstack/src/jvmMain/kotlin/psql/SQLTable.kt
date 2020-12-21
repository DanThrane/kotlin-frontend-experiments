package dk.thrane.playground.psql

import dk.thrane.playground.MigrationHandler

abstract class SQLTable(val tableName: String) {
    override fun toString() = tableName
    open fun registerMigrations(handler: MigrationHandler) {}
}
