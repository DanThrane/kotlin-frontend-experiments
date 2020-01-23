package dk.thrane.playground

import dk.thrane.playground.modules.Module
import java.util.concurrent.atomic.AtomicInteger

class LocalConnection(module: Module) : Connection {
    private val requestId = AtomicInteger(0)
    override suspend fun retrieveRequestId(): Int = requestId.incrementAndGet()
    private val controllersByNamespace = module.controllers
        .flatMap { controller ->
            controller.namespaces.map { ns ->
                ns to controller
            }
        }
        .toMap()

    override suspend fun <Request, Response> call(
        rpc: RPC<Request, Response>,
        connectionWithAuth: ConnectionWithAuthorization,
        header: RequestHeader,
        request: Request
    ): Response {
        val controller =
            controllersByNamespace[rpc.namespace] ?: throw IllegalArgumentException("Unknown namespace for RPC: '$rpc'")

        val (_, handler) = controller.findHandler(rpc.requestName)
            ?: throw IllegalArgumentException("No handler registered in $controller for rpc: $rpc")

        val (responseCode, response) = handler(
            RPCHandlerContextImpl(
                request,
                connectionWithAuth.authorization,
                "local-connection",
                rpc
            )
        )

        if (responseCode != ResponseCode.OK) {
            throw RPCException(responseCode, "Error! $responseCode")
        } else {
            @Suppress("UNCHECKED_CAST")
            return response as Response
        }
    }
}
