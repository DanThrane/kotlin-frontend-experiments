package dk.thrane.playground.modules

import dk.thrane.playground.MigrationHandler

class MigrationPlugin(private val container: ModuleContainer) : ContainerPlugin {
    init {
        container.migrationHandler = MigrationHandler(container.pgPool)
    }

    companion object : ContainerPluginFactory<MigrationPlugin> {
        override val key: AttributeKey<MigrationPlugin> = AttributeKey("migration")
        override fun createPlugin(container: ModuleContainer): MigrationPlugin = MigrationPlugin(container)

        internal val migrationHandler = AttributeKey<MigrationHandler>("migration-handler")
    }
}

var ModuleContainer.migrationHandler: MigrationHandler
    get() = attributes[MigrationPlugin.migrationHandler]
    private set(value) {
        attributes[MigrationPlugin.migrationHandler] = value
    }
