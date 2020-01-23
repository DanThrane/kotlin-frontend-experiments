package dk.thrane.playground.modules

interface ContainerPlugin

interface ContainerPluginFactory<Plugin : ContainerPlugin> {
    val key: AttributeKey<Plugin>

    fun createPlugin(container: ModuleContainer): Plugin
}
