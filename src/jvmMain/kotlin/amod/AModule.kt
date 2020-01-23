package dk.thrane.playground.amod

import dk.thrane.playground.Controller
import dk.thrane.playground.Log
import dk.thrane.playground.modules.Module
import dk.thrane.playground.modules.ModuleContainer

class AModule : Module {
    override val controllers = ArrayList<Controller>()
    private val log = Log("AModule")

    override fun init(container: ModuleContainer) {
        log.info("init!")
        controllers.add(AController())
    }
}
