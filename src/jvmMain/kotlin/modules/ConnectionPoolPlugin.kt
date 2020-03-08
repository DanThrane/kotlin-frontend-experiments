package dk.thrane.playground.modules

import dk.thrane.playground.*

class ConnectionPoolPlugin(private val container: ModuleContainer) : ContainerPlugin {
    private val remoteConnections = HashMap<LocatedService.Remote, WSConnectionPool>()
    private val localConnections by lazy { container.modules.map { it to LocalConnection(it) }.toMap() }
    private val serviceDiscovery = container.getPlugin(ServiceDiscoveryPlugin)

    // TODO This needs to respect the virtual connections

    suspend fun <Request, Response> call(
        rpc: RPC<Request, Response>,
        vcWithAuth: VCWithAuth,
        request: Request
    ): Response {
        when (val location = serviceDiscovery.locateNamespace(rpc.namespace)) {
            is LocatedService.Remote -> {
                TODO()
            }

            is LocatedService.Local -> {
                val conn = localConnections.getValue(location.module)
                val connectionWithAuth =
                    ConnectionWithAuthorization(conn, vcWithAuth.virtualConnection, vcWithAuth.authorization)
                return rpc.call(connectionWithAuth, request)
            }
        }
    }

    companion object : ContainerPluginFactory<ConnectionPoolPlugin> {
        override val key = AttributeKey<ConnectionPoolPlugin>("connection-pool")
        override fun createPlugin(container: ModuleContainer): ConnectionPoolPlugin = ConnectionPoolPlugin(container)
    }
}

suspend inline fun <Req, Res> RPC<Req, Res>.call(
    connPool: ConnectionPoolPlugin,
    vcWithAuth: VCWithAuth,
    request: Req
): Res {
    return connPool.call(this, vcWithAuth, request)
}
