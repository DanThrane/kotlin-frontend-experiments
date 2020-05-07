package dk.thrane.playground.modules

import dk.thrane.playground.AttributeKey

interface ContainerPlugin

interface ContainerPluginFactory<Plugin : ContainerPlugin> {
    val key: AttributeKey<Plugin>

    fun createPlugin(container: ModuleContainer): Plugin
}
