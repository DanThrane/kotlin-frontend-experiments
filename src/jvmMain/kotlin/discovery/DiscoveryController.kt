package dk.thrane.playground.discovery

import dk.thrane.playground.Controller
import dk.thrane.playground.EmptyMessage
import dk.thrane.playground.respond

class DiscoveryController(private val service: DiscoveryService) : Controller() {
    override fun configureController() {
        implement(Discovery.destroy) {
            service.delete(request)
            respond(EmptyMessage)
        }

        implement(Discovery.register) {
            service.insert(request)
            respond(EmptyMessage)
        }

        implement(Discovery.ping) {
            service.ping(request)
            respond(EmptyMessage)
        }

        implement(Discovery.search) {
            respond(
                ServiceQueryResponse(
                    service.findByNamespace(request.namespace)
                )
            )
        }
    }
}
