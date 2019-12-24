package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.db.DBConnectionPool
import dk.thrane.playground.db.DatabaseConfig
import dk.thrane.playground.site.api.PrincipalRole
import dk.thrane.playground.site.service.*
import kotlinx.coroutines.runBlocking

class Main(args: Array<String>) : BaseServer() {
    init {
        val dbPool = DBConnectionPool(DatabaseConfig("kotlin", "kotlin", "kotlin", "localhost"))
        val migrations = MigrationHandler(dbPool)
        Principals.migration(migrations)
        Tokens.migration(migrations)

        if ("--migrate" in args || true) {
            runBlocking { migrations.runMigrations() }
        }

        val authService = AuthenticationService(dbPool)

        runBlocking { authService.createUser(PrincipalRole.ADMIN, "foo", "bar") }

        repeat(10) {
            runBlocking { authService.createUser(PrincipalRole.ADMIN, "u$it", "bar") }
        }

        addController(AuthenticationController(authService))
    }
}

fun main(args: Array<String>) {
    val server = Main(args)
    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
