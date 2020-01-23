package dk.thrane.playground.modules

sealed class LocatedService {
    data class Local(val module: Module) : LocatedService()
    data class Remote(val host: String, val port: Int) : LocatedService()
}
