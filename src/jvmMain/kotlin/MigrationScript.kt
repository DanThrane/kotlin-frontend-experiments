package dk.thrane.playground

import dk.thrane.playground.db.AsyncDBConnection
import dk.thrane.playground.db.DBConnectionPool
import dk.thrane.playground.db.SQLRow
import dk.thrane.playground.db.SQLTable
import dk.thrane.playground.db.creationScript
import dk.thrane.playground.db.insert
import dk.thrane.playground.db.int
import dk.thrane.playground.db.sendPreparedStatement
import dk.thrane.playground.db.transaction
import dk.thrane.playground.db.useInstance
import dk.thrane.playground.db.varchar

data class MigrationScript(
    val name: String,
    val script: suspend (AsyncDBConnection) -> Unit
)

object MigrationTable : SQLTable("migration") {
    val index = int("index")
    val scriptName = varchar("name", 256)

    override fun migration(handler: MigrationHandler) {
        // Do nothing
    }
}

class MigrationHandler(private val db: DBConnectionPool) {
    private val migrations = ArrayList<MigrationScript>()

    fun addScript(name: String, script: suspend (AsyncDBConnection) -> Unit) {
        migrations.add(MigrationScript(name, script))
    }

    suspend fun runMigrations() {
        db.useInstance { conn ->
            log.debug("Starting migration")

            val tableExists = conn.sendPreparedStatement(
                {
                    setParameter("table", MigrationTable.name)
                    setParameter("schema", db.schema)
                },

                """
                    select exists(
                        select 1
                        from information_schema.tables 
                        where  table_schema = ?schema
                        and    table_name = ?table
                    )
                """
            ).rows.singleOrNull()?.getBoolean(0) ?: false

            if (!tableExists) {
                db.transaction(conn) {
                    log.info("Migration meta table not found. Creating a new table!")
                    conn.sendPreparedStatement(MigrationTable.creationScript())
                }
            } else {
                log.debug("Migration meta table already exists.")
            }

            val maxIndex = db.transaction(conn) {
                conn
                    .sendPreparedStatement("select max(${MigrationTable.index}) from $MigrationTable")
                    .rows.singleOrNull()?.getInt(0) ?: 0
            }

            log.debug("Migration up to index $maxIndex has been completed.")

            migrations.forEachIndexed { index, migration ->
                if (index + 1 > maxIndex) {
                    db.transaction(conn) {
                        log.info("Running migration: ${migration.name}")
                        migration.script(conn)
                        conn.insert(MigrationTable, SQLRow().also { row ->
                            row[MigrationTable.index] = index + 1
                            row[MigrationTable.scriptName] = migration.name
                        })
                    }
                }
            }

            log.info("Migration complete")
        }
    }

    companion object {
        private val log = Log("MigrationHandler")
    }
}
