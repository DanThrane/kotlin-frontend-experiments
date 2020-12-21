package dk.thrane.playground.site.auth

import dk.thrane.playground.Controller
import dk.thrane.playground.HS256WithKey
import dk.thrane.playground.JWT
import dk.thrane.playground.default
import dk.thrane.playground.modules.Module
import dk.thrane.playground.modules.ModuleContainer
import dk.thrane.playground.modules.migrationHandler
import dk.thrane.playground.modules.pgPool
import dk.thrane.playground.site.api.PrincipalRole
import kotlinx.coroutines.runBlocking

class AuthenticationModule : Module {
    override val controllers: MutableList<Controller> = ArrayList()

    override fun init(container: ModuleContainer) {
        PrincipalTable.registerMigrations(container.migrationHandler)
        TokenTable.registerMigrations(container.migrationHandler)

        val authenticationService = AuthenticationService(
            container.pgPool,
            JWT.default,
            HS256WithKey("test")
        )

        controllers.add(
            AuthenticationController(
                authenticationService
            )
        )
    }
}
