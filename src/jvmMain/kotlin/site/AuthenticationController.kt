package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.site.api.*
import dk.thrane.playground.site.service.AuthenticationService

class AuthenticationController(
    private val authenticationService: AuthenticationService
) : Controller() {
    override fun configureController() {
        implement(Authentication.login) {
            val resp = authenticationService.login(
                request.username,
                request.password
            ) ?: throw RPCException(ResponseCode.FORBIDDEN, "Invalid username or password")

            respond(LoginResponse(resp.token))
        }

        implement(Authentication.logout) {
            authenticationService.logout(request.token)
            respond(EmptyMessage)
        }

        implement(Authentication.whoami) {
            val principal = authenticationService.validateToken(authorization) ?:
                throw RPCException(ResponseCode.UNAUTHORIZED, "Not authenticated with service.")
            Pair(ResponseCode.OK, Principal(principal.username, principal.role))
        }
    }
}
