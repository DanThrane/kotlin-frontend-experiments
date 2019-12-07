package dk.thrane.playground.site

import dk.thrane.playground.Controller
import dk.thrane.playground.RPCException
import dk.thrane.playground.ResponseCode
import dk.thrane.playground.respond
import dk.thrane.playground.site.api.*
import dk.thrane.playground.site.service.AuthenticationService

class AuthenticationController(
    private val authenticationService: AuthenticationService
) : Controller() {
    override fun configureController() {
        implement(Authentication.login) {
            val resp = authenticationService.login(
                request[LoginRequest.username],
                request[LoginRequest.password]
            ) ?: throw RPCException(ResponseCode.FORBIDDEN, "Invalid username or password")

            Pair(ResponseCode.OK, LoginResponse(resp.token))
        }

        implement(Authentication.logout) {
            authenticationService.logout(request[LogoutRequest.token])
            respond {}
        }

        implement(Authentication.whoami) {
            val principal = authenticationService.validateToken(authorization) ?:
                throw RPCException(ResponseCode.UNAUTHORIZED, "Not authenticated with service.")
            Pair(ResponseCode.OK, PrincipalSchema(principal.username, principal.role.name))
        }
    }
}
