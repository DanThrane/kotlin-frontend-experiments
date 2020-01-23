package dk.thrane.playground.bmod

import dk.thrane.playground.Controller
import dk.thrane.playground.modules.Module
import dk.thrane.playground.modules.ModuleContainer

class BModule : Module {
    override val controllers = ArrayList<Controller>()

    override fun init(container: ModuleContainer) {
        controllers.add(BController())
    }
}
