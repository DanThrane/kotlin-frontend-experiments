package dk.thrane.playground

import java.sql.Connection

data class MigrationScript(
    val name: String,
    val script: (Connection) -> Unit
)

object MigrationTable : SQLTable("migration") {
    val index = int("index")
    val scriptName = varchar("name", 256)
}

class MigrationHandler(private val db: ConnectionPool) {
    private val migrations = ArrayList<MigrationScript>()

    fun addScript(name: String, script: (Connection) -> Unit) {
        migrations.add(MigrationScript(name, script))
    }

    fun runMigrations() {
        db.useInstance { conn ->
            log.debug("Starting migration")

            val tablesFound = conn.metaData
                .getTables(null, null, MigrationTable.name.toUpperCase(), arrayOf("TABLE"))
                .enhance()
                .mapToResult { row ->
                    row.getString("TABLE_NAME")
                }

            if (tablesFound.isEmpty()) {
                log.info("Migration meta table not found. Creating a new table!")
                conn.prepareStatement(MigrationTable.creationScript()).executeUpdate()
                conn.commit()
            } else {
                log.debug("Migration meta table already exists.")
            }

            val maxIndex = conn
                .prepareStatement("select max(${MigrationTable.index}) from $MigrationTable")
                .mapQuery { it.getInt(1) }
                .singleOrNull() ?: 0

            log.debug("Migration up to index $maxIndex has been completed.")

            migrations.forEachIndexed { index, migration ->
                if (index + 1 > maxIndex) {
                    log.info("Running migration: ${migration.name}")
                    migration.script(conn)
                    conn.insert(MigrationTable, listOf(SQLRow().also { row ->
                        row[MigrationTable.index] = index + 1
                        row[MigrationTable.scriptName] = migration.name
                    }))
                    conn.commit()
                }
            }

            log.info("Migration complete")
        }
    }

    companion object {
        private val log = Log("MigrationHandler")
    }
}
