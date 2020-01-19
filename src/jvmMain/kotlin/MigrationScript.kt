package dk.thrane.playground

import dk.thrane.playground.psql.SQLTable
import dk.thrane.playground.psql.*
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.Serializable

data class MigrationScript(
    val name: String,
    val script: suspend (PostgresConnection) -> Unit
)

@Serializable
data class MigrationTable(val index: Int, val scriptName: String) {
    companion object : SQLTable("migrations") {
        const val index = "index"
        const val scriptName = "scriptName"
    }
}

class MigrationHandler(private val db: PostgresConnectionPool) {
    private val migrations = ArrayList<MigrationScript>()

    fun addScript(name: String, script: suspend (PostgresConnection) -> Unit) {
        migrations.add(MigrationScript(name, script))
    }

    @Serializable private data class CountTable(val count: Long?)

    @Serializable private data class FindTableQuery(val schema: String, val table: String)
    @Serializable private data class FindTableResponse(val exists: Boolean)
    private val findTable = PreparedStatement(
        """
            select exists(
                select 1
                from information_schema.tables 
                where  table_schema = ?schema
                and    table_name = ?table
            )
        """,
        FindTableQuery.serializer(),
        FindTableResponse.serializer()
    ).asQuery()

    private val insertMigration = PreparedStatement(
        """
            insert into $MigrationTable 
                (${MigrationTable.index}, ${MigrationTable.scriptName}) 
                values (?${MigrationTable.index}, ?${MigrationTable.scriptName})
        """,
        MigrationTable.serializer(),
        EmptyTable.serializer()
    ).asCommand()

    suspend fun runMigrations() {
        db.useInstance { conn ->
            log.debug("Starting migration")

            val tableExists = findTable(conn, FindTableQuery("public", "migrations")).singleOrNull()?.exists ?: false

            if (!tableExists) {
                conn.withTransaction {
                    log.info("Migration meta table not found. Creating a new table!")
                    conn.sendCommand(
                        """
                            create table migrations(
                                index int4,
                                scriptName text
                            )
                        """
                    )
                }
            } else {
                log.debug("Migration meta table already exists.")
            }

            val maxIndex = conn.withTransaction {
                conn
                    .sendQuery("select max(${MigrationTable.index})::int8 from $MigrationTable")
                    .mapRows(CountTable.serializer())
                    .singleOrNull()?.count ?: 0L
            }

            log.debug("Migration up to index $maxIndex has been completed.")

            migrations.forEachIndexed { index, migration ->
                if (index + 1 > maxIndex) {
                    conn.withTransaction {
                        log.info("Running migration: ${migration.name}")
                        migration.script(conn)
                        insertMigration(conn, MigrationTable(index + 1, migration.name))
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

