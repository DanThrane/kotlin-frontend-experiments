package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.site.api.PrincipalRole
import dk.thrane.playground.site.service.*

class Main(args: Array<String>) : BaseServer() {
    init {
        val dbPool = DBConnectionPool("org.h2.Driver", "jdbc:h2:mem:data;DB_CLOSE_DELAY=-1")

        val migrations = MigrationHandler(dbPool)
        Principals.migration(migrations)
        Tokens.migration(migrations)

        if ("--migrate" in args || true) {
            migrations.runMigrations()
        }

        val authService = AuthenticationService(dbPool)

        authService.createUser(PrincipalRole.ADMIN, "foo", "bar")

        repeat(10) {
            authService.createUser(PrincipalRole.ADMIN, "u$it", "bar")
        }

        addController(AuthenticationController(authService))
    }
}

fun main(args: Array<String>) {
    val server = Main(args)
    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
