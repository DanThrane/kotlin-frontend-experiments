package dk.thrane.playground.bmod

import dk.thrane.playground.Controller
import dk.thrane.playground.respond

class BController : Controller() {
    override fun configureController() {
        implement(BNamespace.bar) {
            respond(Unit)
        }
    }

}
