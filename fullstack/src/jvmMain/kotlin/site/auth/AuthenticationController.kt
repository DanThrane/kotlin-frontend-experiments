package dk.thrane.playground.site.auth

import dk.thrane.playground.*
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
            log.info("Received this JWT: $jwtToken")
            val principal = authenticationService.validateJWT(jwtToken)
                ?: throw RPCException(ResponseCode.UNAUTHORIZED, "Not authenticated with service.")
            log.info("It all checks out: ${principal}")
            Pair(ResponseCode.OK, Principal(principal.username, principal.role))
        }

        implement(Authentication.refresh) {
            val refreshToken = request.token
            respond(RefreshResponse(authenticationService.refresh(refreshToken)))
        }
    }

    companion object {
        private val log = Log("AuthenticationController")
    }
}
