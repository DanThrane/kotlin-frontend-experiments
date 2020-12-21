package dk.thrane.playground.site.testing

import dk.thrane.playground.*
import dk.thrane.playground.modules.Module
import dk.thrane.playground.modules.ModuleContainer
import dk.thrane.playground.site.testing.Testing

class TestingModule : Module, Controller() {
    override val controllers = listOf(this)

    override fun init(container: ModuleContainer) {
    }

    override fun configureController() {
        implement(Testing.oneWay) {
            respond(EmptyMessage)
        }

        implement(Testing.requestResponse) {
            respond(request)
        }

        implement(Testing.error) {
            throw RPCException(ResponseCode.INTERNAL_ERROR, "ERROR!")
        }
    }
}
