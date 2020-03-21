package dk.thrane.playground.site.auth

import dk.thrane.playground.Controller
import dk.thrane.playground.EmptyMessage
import dk.thrane.playground.RPCException
import dk.thrane.playground.ResponseCode
import dk.thrane.playground.respond
import dk.thrane.playground.site.api.Authentication
import dk.thrane.playground.site.api.LoginResponse
import dk.thrane.playground.site.api.Principal
import dk.thrane.playground.site.api.RefreshResponse

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
            val jwtToken = authorization ?: throw RPCException(ResponseCode.UNAUTHORIZED, "Missing token")
            val principal = authenticationService.validateJWT(jwtToken)
                ?: throw RPCException(ResponseCode.UNAUTHORIZED, "Not authenticated with service.")
            Pair(ResponseCode.OK, Principal(principal.username, principal.role))
        }

        implement(Authentication.refresh) {
            val refreshToken = request.token
            respond(RefreshResponse(authenticationService.refresh(refreshToken)))
        }
    }
}
