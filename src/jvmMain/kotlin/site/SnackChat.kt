package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.site.api.PrincipalRole
import dk.thrane.playground.site.api.SnackTag
import dk.thrane.playground.site.service.*

class SnackChat(args: Array<String>) : BaseServer() {
    init {
        val dbPool = DBConnectionPool("org.h2.Driver", "jdbc:h2:mem:data;DB_CLOSE_DELAY=-1")

        val migrations = MigrationHandler(dbPool)
        Principals.migration(migrations)
        Tokens.migration(migrations)
        SnackerTags.migration(migrations)
        SnackerFollowers.migration(migrations)

        if ("--migrate" in args || true) {
            migrations.runMigrations()
        }

        val authService = AuthenticationService(dbPool)
        val followerDao = FollowerDao()
        val tagDao = UserTagDao()
        val snackers = SnackerService(dbPool, followerDao, tagDao)

        authService.createUser(PrincipalRole.ADMIN, "foo", "bar")

        repeat(10) {
            authService.createUser(PrincipalRole.ADMIN, "u$it", "bar")
        }

        dbPool.useInstance { conn ->
            tagDao.setTagsForUser(conn, "foo", setOf(SnackTag.BURGER, SnackTag.GRAPES))
        }

        addController(AuthenticationController(authService))
        addController(SnackerController(authService, snackers))
    }
}

fun main(args: Array<String>) {
    val server = SnackChat(args)
    startServer(httpRequestHandler = server, webSocketRequestHandler = server)
}
