package dk.thrane.playground.modules

import dk.thrane.playground.Controller

interface Module {
    fun init(container: ModuleContainer)
    val controllers: List<Controller>
}
