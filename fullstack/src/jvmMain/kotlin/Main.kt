package dk.thrane.playground

import dk.thrane.playground.modules.*
import dk.thrane.playground.site.auth.AuthenticationModule
import dk.thrane.playground.site.testing.TestingModule
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    runBlocking {
        val container = ModuleContainer(args.toList() + listOf("--migrate")).apply {
            LogManager.customLogLevels["PostgresConnection"] = LogLevel.INFO
            LogManager.customLogLevels["PSQLMessage"] = LogLevel.INFO

            install(PostgresPlugin)
            install(MigrationPlugin)

            install(AuthenticationModule())
            install(TestingModule())
        }

        container.start().join()
    }
}
