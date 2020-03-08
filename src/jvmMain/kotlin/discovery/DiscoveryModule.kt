package dk.thrane.playground.discovery

import dk.thrane.playground.Controller
import dk.thrane.playground.modules.Module
import dk.thrane.playground.modules.ModuleContainer
import dk.thrane.playground.modules.migrationHandler
import dk.thrane.playground.modules.pgPool

class DiscoveryModule : Module {
    override val controllers = ArrayList<Controller>()

    override fun init(container: ModuleContainer) {
        val service = DiscoveryService(container.pgPool, container.migrationHandler)
        controllers.add(DiscoveryController(service))
    }
}
