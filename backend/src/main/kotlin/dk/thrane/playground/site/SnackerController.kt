package dk.thrane.playground.site

import dk.thrane.playground.*
import dk.thrane.playground.site.api.*
import dk.thrane.playground.site.service.AuthenticationService
import dk.thrane.playground.site.service.FollowerService
import dk.thrane.playground.site.service.verifyUser

class SnackerController(
    private val authentication: AuthenticationService,
    private val followers: FollowerService
) : Controller() {
    override fun configureController() {
        implement(Snackers.toggleFollow, FindByString) {
            val user = authentication.verifyUser(authorization)
            followers.follow(user, mappedRequest.id)
            ResponseCode.OK to EmptyOutgoingMessage()
        }

        implement(Snackers.viewFollowers, ViewFollowersRequest) {
            val user = authentication.verifyUser(authorization)
            val followers = followers.viewFollowers(user, mappedRequest.pagination)
            respond(Followers(user.username, followers))
        }

        implement(Snackers.whoami) {
            TODO()
        }

        implement(Snackers.view) {
            TODO()
        }
    }
}
