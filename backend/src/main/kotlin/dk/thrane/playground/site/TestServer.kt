package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.site.service.AuthenticationService
import dk.thrane.playground.site.service.Principals
import dk.thrane.playground.site.service.Tokens

class TestServer(args: Array<String>) : BaseServer() {
    init {
        val dbPool = ConnectionPool("org.h2.Driver", "jdbc:h2:mem:data;DB_CLOSE_DELAY=-1")

        val migrations = MigrationHandler(dbPool)
        migrations.addScript("test") { conn ->
            conn.prepareStatement("create table foo(bar int);").executeUpdate()
        }
        Principals.migration(migrations)
        Tokens.migration(migrations)

        if ("--migrate" in args || true) {
            migrations.runMigrations()
        }

        val authService = AuthenticationService(dbPool)

        addController(AuthenticationController(authService))
        addController(CourseController(authService))
    }
}

fun main(args: Array<String>) {
    val server = TestServer(args)
    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
