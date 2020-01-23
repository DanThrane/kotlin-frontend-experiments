package dk.thrane.playground

import dk.thrane.playground.amod.AModule
import dk.thrane.playground.amod.ANamespace
import dk.thrane.playground.bmod.BModule
import dk.thrane.playground.modules.ConnectionPoolPlugin
import dk.thrane.playground.modules.ModuleContainer
import dk.thrane.playground.modules.ServiceDiscoveryPlugin
import dk.thrane.playground.modules.call

suspend fun main(args: Array<String>) {
    val container = ModuleContainer(args.toList()).apply {
        install(ServiceDiscoveryPlugin)
        install(ConnectionPoolPlugin)

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
