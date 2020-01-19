package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.psql.PostgresConnectionParameters
import dk.thrane.playground.psql.PostgresConnectionPool
import dk.thrane.playground.site.api.PrincipalRole
import dk.thrane.playground.site.service.*
import kotlinx.coroutines.runBlocking

class Main(args: Array<String>) : BaseServer() {
    init {
        val dbPool = PostgresConnectionPool(PostgresConnectionParameters("kotlin", "kotlin", "kotlin", "localhost"))
        val migrations = MigrationHandler(dbPool)
        PrincipalTable.registerMigrations(migrations)
        TokenTable.registerMigrations(migrations)

        if ("--migrate" in args || true) {
            runBlocking { migrations.runMigrations() }
        }

        val authService = AuthenticationService(dbPool, JWT.default, HS256WithKey("test"))

        /*
        runBlocking { authService.createUser(PrincipalRole.ADMIN, "foo", "bar") }

        repeat(10) {
            runBlocking { authService.createUser(PrincipalRole.ADMIN, "u$it", "bar") }
        }
         */

        addController(AuthenticationController(authService))
    }
}

fun main(args: Array<String>) {
    val server = Main(args)
    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
