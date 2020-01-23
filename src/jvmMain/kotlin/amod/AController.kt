package dk.thrane.playground.amod

import dk.thrane.playground.Controller
import dk.thrane.playground.respond

class AController : Controller() {
    override fun configureController() {
        implement(ANamespace.foo) {
            respond(Unit)
        }
    }
}
