package dk.thrane.playground

import dk.thrane.playground.amod.AModule
import dk.thrane.playground.amod.ANamespace
import dk.thrane.playground.bmod.BModule
import dk.thrane.playground.modules.*

suspend fun main(args: Array<String>) {
    val container = ModuleContainer(args.toList()).apply {
        install(ServiceDiscoveryPlugin)
        install(ConnectionPoolPlugin)
        install(PostgresPlugin)
        install(MigrationPlugin)

        // Cost of reflection is around 200ms for a small codebase
        install(AModule())
        install(BModule())
    }

    val job = container.start()
    val connPool = container.getPlugin(ConnectionPoolPlugin)
    repeat(100) {
        ANamespace.foo.call(connPool, VCWithAuth(STATELESS_CONNECTION), Unit)
    }

    job.cancel()
}
