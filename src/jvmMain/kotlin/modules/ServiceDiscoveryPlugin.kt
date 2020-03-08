package dk.thrane.playground.modules

import dk.thrane.playground.STATELESS_CONNECTION
import dk.thrane.playground.VCWithAuth
import dk.thrane.playground.discovery.Discovery
import dk.thrane.playground.discovery.DiscoveryModule
import dk.thrane.playground.discovery.ServiceLocation
import kotlinx.coroutines.runBlocking

class ServiceDiscoveryPlugin(private val container: ModuleContainer) : ContainerPlugin {
    init {
        container.install(DiscoveryModule())
        container.addOnStartScript {
            val connPool = container.getPlugin(ConnectionPoolPlugin)
            runBlocking {
                Discovery.register.call(
                    connPool,
                    VCWithAuth(STATELESS_CONNECTION),
                    ServiceLocation(
                        "???",
                        1,
                        container.modules.flatMap { mod -> mod.controllers.flatMap { it.namespaces } }
                    )
                )
            }
        }
    }

    suspend fun locateNamespace(namespace: String): LocatedService {
        val module = container.modules.find { mod ->
            mod.controllers
                .flatMap { it.namespaces }
                .any { it == namespace }
        }

        if (module != null) {
            return LocatedService.Local(module)
        } else {
            TODO()
        }
    }

    companion object : ContainerPluginFactory<ServiceDiscoveryPlugin> {
        override val key: AttributeKey<ServiceDiscoveryPlugin> = AttributeKey("service-discovery")
        override fun createPlugin(container: ModuleContainer): ServiceDiscoveryPlugin =
            ServiceDiscoveryPlugin(container)
    }
}
