package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.site.api.*
import dk.thrane.playground.site.service.AuthenticationService
import dk.thrane.playground.site.service.SnackerService
import dk.thrane.playground.site.service.verifyUser

class SnackerController(
    private val authentication: AuthenticationService,
    private val snackers: SnackerService
) : Controller() {
    override fun configureController() {
        implement(Snackers.toggleFollow, FindByString) {
            val user = authentication.verifyUser(authorization)

            snackers.toggleFollowing(user, mappedRequest.id)
            ResponseCode.OK to EmptyOutgoingMessage()
        }

        implement(Snackers.viewFollowers, ViewFollowersRequest) {
            authentication.verifyUser(authorization)

            val followers = snackers.viewFollowers(mappedRequest.username, mappedRequest.pagination)
            respond(Followers(mappedRequest.username, followers))
        }

        implement(Snackers.whoami) {
            val user = authentication.verifyUser(authorization)
            respond(snackers.viewSnacker(user.username))
        }

        implement(Snackers.view, FindByString) {
            log.info("Auth token: $authorization")
            authentication.verifyUser(authorization)
            respond(snackers.viewSnacker(mappedRequest.id))
        }
    }

    companion object {
        private val log = Log("SnackerController")
    }
}
