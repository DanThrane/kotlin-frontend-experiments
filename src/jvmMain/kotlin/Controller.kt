package dk.thrane.playground

abstract class Controller {
    private var isConfiguring = true
    private val handlers = ArrayList<Pair<RPC<*, *>, UntypedRPChandler>>()

    fun <Req, Res> implement(
        rpc: RPC<Req, Res>,
        handler: RPCHandler<Req, Res>
    ) {
        check(isConfiguring)

        @Suppress("UNCHECKED_CAST")
        handlers.add(rpc to handler as UntypedRPChandler)
    }

    fun findHandler(requestName: String): Pair<RPC<*, *>, UntypedRPChandler>? {
        return handlers.find { it.first.requestName == requestName }
    }

    fun configure() {
        isConfiguring = true
        configureController()
        isConfiguring = false
    }

    protected abstract fun configureController()
}
