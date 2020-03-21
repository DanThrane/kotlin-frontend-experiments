package dk.thrane.playground.site.auth

import dk.thrane.playground.Controller
import dk.thrane.playground.HS256WithKey
import dk.thrane.playground.JWT
import dk.thrane.playground.default
import dk.thrane.playground.modules.Module
import dk.thrane.playground.modules.ModuleContainer
import dk.thrane.playground.modules.migrationHandler
import dk.thrane.playground.modules.pgPool

class AuthenticationModule : Module {
    override val controllers: MutableList<Controller> = ArrayList()

    override fun init(container: ModuleContainer) {
        PrincipalTable.registerMigrations(container.migrationHandler)
        TokenTable.registerMigrations(container.migrationHandler)

        controllers.add(
            AuthenticationController(
                AuthenticationService(
                    container.pgPool,
                    JWT.default,
                    HS256WithKey("test")
                )
            )
        )
    }
}
