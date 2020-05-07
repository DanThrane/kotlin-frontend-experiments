package dk.thrane.playground.modules

import dk.thrane.playground.AttributeKey
import dk.thrane.playground.psql.PostgresConnectionParameters
import dk.thrane.playground.psql.PostgresConnectionPool

class PostgresPlugin(private val container: ModuleContainer) : ContainerPlugin {
    init {
        container.pgPool = PostgresConnectionPool(
            PostgresConnectionParameters(
                username = "kotlin",
                password = "kotlin",
                database = "kotlin",
                hostname = "localhost"
            )
        )
    }

    companion object : ContainerPluginFactory<PostgresPlugin> {
        override val key: AttributeKey<PostgresPlugin> = AttributeKey("postgres")
        override fun createPlugin(container: ModuleContainer): PostgresPlugin = PostgresPlugin(container)
        internal val connectionPool = AttributeKey<PostgresConnectionPool>("postgres-pool")
    }
}

var ModuleContainer.pgPool: PostgresConnectionPool
    get() = attributes[PostgresPlugin.connectionPool]
    private set(value) {
        attributes[PostgresPlugin.connectionPool] = value
    }
