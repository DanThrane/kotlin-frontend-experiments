package dk.thrane.playground.discovery

import dk.thrane.playground.MigrationHandler
import dk.thrane.playground.namespaced
import dk.thrane.playground.psql.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable

class DiscoveryService(
    private val db: PostgresConnectionPool,
    migrations: MigrationHandler
) {
    @Serializable
    data class DiscoveryTable(
        val hostname: String,
        val port: Int,
        val namespace: String,
        val lastUpdate: Long
    ) {
        companion object : SQLTable("discovery") {
            const val hostname = "hostname"
            const val port = "port"
            const val namespace = "namespace"
            const val lastUpdate = "lastUpdate"
        }
    }

    init {
        migrations.namespaced("discovery") {
            addScript("table init") { conn ->
                conn.sendCommand(
                    """
                        create table ${DiscoveryTable}(
                            ${DiscoveryTable.hostname} text,
                            ${DiscoveryTable.port} int4,
                            ${DiscoveryTable.namespace} text,
                            ${DiscoveryTable.lastUpdate} int8
                            primary key(${DiscoveryTable.hostname}, ${DiscoveryTable.port}, ${DiscoveryTable.namespace})
                        )
                    """
                )

                conn.sendCommand(
                    """
                        create index ns_idx on $DiscoveryTable (${DiscoveryTable.namespace})  
                    """
                )
            }
        }
    }

    @Serializable
    data class PingCommandInput(
        val lastUpdate: Long,
        val hostname: String,
        val port: Int,
        val namespaces: List<String>
    )

    private val pingCommand = PreparedStatement(
        """
            update $DiscoveryTable
            set
                ${DiscoveryTable.lastUpdate} = ?lastUpdate
            where
                ${DiscoveryTable.hostname} = ?hostname and
                ${DiscoveryTable.port} = ?port and
                ${DiscoveryTable.namespace} in (select unnest(?namespaces::text[]))
        """,
        PingCommandInput.serializer(),
        EmptyTable.serializer()
    ).asCommand()

    private val insertCommand = createInsertStatement(DiscoveryTable, DiscoveryTable.serializer()).asBatchedCommand()

    @Serializable
    data class HostnameAndPort(val hostname: String, val port: Int)

    private val deleteCommand = PreparedStatement(
        """
            delete from $DiscoveryTable
            where
                ${DiscoveryTable.hostname} = ?hostname
                ${DiscoveryTable.port} = ?port
        """,
        HostnameAndPort.serializer(),
        EmptyTable.serializer()
    ).asBatchedCommand()

    @Serializable
    data class FindByNamespace(val namespace: String, val lastUpdate: Long)

    private val findByNamespaceQuery = PreparedStatement(
        """
            select *
            from $DiscoveryTable
            where 
                ${DiscoveryTable.namespace} = ?namespace and
                ${DiscoveryTable.lastUpdate} >= ?lastUpdate
        """,
        FindByNamespace.serializer(),
        DiscoveryTable.serializer()
    ).asQuery()

    suspend fun insert(serviceLocation: ServiceLocation) {
        db.useTransaction { conn ->
            val now = System.currentTimeMillis()
            insertCommand(conn, serviceLocation.namespaces.asFlow().map { ns ->
                DiscoveryTable(serviceLocation.hostname, serviceLocation.port, ns, now)
            })
        }
    }

    suspend fun ping(serviceLocation: ServiceLocation) {
        db.useTransaction { conn ->
            pingCommand(
                conn,
                PingCommandInput(
                    System.currentTimeMillis(),
                    serviceLocation.hostname,
                    serviceLocation.port,
                    serviceLocation.namespaces
                )
            )
        }
    }

    suspend fun delete(serviceLocation: ServiceLocation) {
        db.useTransaction { conn ->
            deleteCommand(conn, flowOf(HostnameAndPort(serviceLocation.hostname, serviceLocation.port)))
        }
    }

    suspend fun findByNamespace(namespace: String): List<ServiceLocation> {
        return db
            .useTransaction { conn ->
                findByNamespaceQuery(
                    conn,
                    FindByNamespace(namespace, System.currentTimeMillis() - maxAge)
                )
            }
            .map { ServiceLocation(it.hostname, it.port, listOf(namespace)) }
            .toList()
    }

    companion object {
        private const val maxAge = 30_000
    }
}
