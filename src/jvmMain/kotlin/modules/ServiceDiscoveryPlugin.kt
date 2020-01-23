package dk.thrane.playground.modules

class ServiceDiscoveryPlugin(private val container: ModuleContainer) : ContainerPlugin {
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
