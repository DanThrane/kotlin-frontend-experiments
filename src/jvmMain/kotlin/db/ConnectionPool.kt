package dk.thrane.playground.db

import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.SuspendingConnectionImpl
import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder
import kotlinx.coroutines.future.await

typealias AsyncDBConnection = SuspendingConnection

data class DatabaseConfig(
    val username: String,
    val password: String,
    val database: String,
    val hostname: String,
    val poolSize: Int = 50,
    val schema: String = "public",
    val port: Int = 5432
)

internal val safeSqlNameRegex = Regex("[a-zA-Z0-9_]+")

class DBConnectionPool(config: DatabaseConfig) {
    val schema = config.schema

    init {
        require(schema.matches(safeSqlNameRegex)) { "Potentially bad schema passed: $schema" }
    }

    private val pool = run {
        val jdbcUrl = "postgresql://${config.hostname}:${config.port}/${config.database}"

        PostgreSQLConnectionBuilder.createConnectionPool(jdbcUrl) {
            this.maxActiveConnections = config.poolSize
            this.maxIdleTime = 30_000
            this.username = config.username
            this.password = config.password
        }
    }

    suspend fun close() {
        pool.disconnect().await()
    }

    suspend fun closeSession(session: AsyncDBConnection) {
        pool.giveBack((session as SuspendingConnectionImpl).connection as PostgreSQLConnection)
    }

    suspend fun commit(session: AsyncDBConnection) {
        session.sendQuery("commit")
    }

    suspend fun openSession(): AsyncDBConnection {
        return pool.take().await().asSuspending
    }

    suspend fun rollback(session: AsyncDBConnection) {
        session.sendQuery("rollback")
    }

    suspend fun openTransaction(session: AsyncDBConnection) {
        // We always begin by setting the search_path to our schema. The schema is checked in the init block to make
        // this safe.
        session.sendQuery("set search_path to $schema")
        session.sendQuery("begin")
    }
}

suspend inline fun <R> DBConnectionPool.useInstance(block: (AsyncDBConnection) -> R): R {
    val session = openSession()
    try {
        return block(session)
    } finally {
        closeSession(session)
    }
}

suspend inline fun <R> DBConnectionPool.transaction(session: AsyncDBConnection, block: () -> R): R {
     try {
         openTransaction(session)
         val result = block()
         commit(session)
         return result
     } catch (ex: Throwable) {
         rollback(session)
         throw ex
     }
}

suspend inline fun <R> DBConnectionPool.useTransaction(block: (AsyncDBConnection) -> R): R {
    return useInstance { conn ->
        transaction(conn) {
            block(conn)
        }
    }
}
