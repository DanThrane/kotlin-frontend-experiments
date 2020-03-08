package dk.thrane.playground

import kotlinx.coroutines.CoroutineScope

object ConnectionController : Controller() {
    private val knownConnections = HashMap<String, Set<Int>>()

    override fun configureController() {
        implement(Connections.open) {
            synchronized(knownConnections) {
                val current = knownConnections[socketId] ?: emptySet()
                knownConnections[socketId] = current + request.id
            }

            respond(EmptyMessage)
        }

        implement(Connections.close) {
            synchronized(knownConnections) {
                val current = knownConnections[socketId] ?: emptySet()
                val newSet = current - request.id
                if (newSet.isNotEmpty()) {
                    knownConnections[socketId] = newSet
                } else {
                    knownConnections.remove(socketId)
                }
            }

            respond(EmptyMessage)
        }
    }

    internal val prehandler: PreHandler = handler@{ rpc, header, message ->
        val socketId = socketId

        val connsForSocket = knownConnections[socketId] ?: emptySet()
        PreHandlerAction.Continue
    }
}
