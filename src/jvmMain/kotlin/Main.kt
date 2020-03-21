package dk.thrane.playground

import dk.thrane.playground.modules.*
import dk.thrane.playground.site.auth.AuthenticationModule
import dk.thrane.playground.site.testing.TestingModule

suspend fun main(args: Array<String>) {
    val container = ModuleContainer(args.toList() + listOf("--migrate")).apply {
        install(PostgresPlugin)
        install(MigrationPlugin)

        install(AuthenticationModule())
        install(TestingModule())
    }

    container.start().join()
}
