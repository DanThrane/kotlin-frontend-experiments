package dk.thrane.playground

import dk.thrane.playground.modules.*

suspend fun main(args: Array<String>) {
    val container = ModuleContainer(args.toList()).apply {
        install(PostgresPlugin)
        install(MigrationPlugin)
    }

    container.start()
}
