package dk.thrane.playground.site.service

import dk.thrane.playground.DBConnectionPool
import dk.thrane.playground.site.api.PaginationRequest
import dk.thrane.playground.site.api.Principal
import dk.thrane.playground.site.api.Snacker
import dk.thrane.playground.useInstance

class SnackerService(
    private val db: DBConnectionPool,
    private val followerDao: FollowerDao,
    private val userTagDao: UserTagDao
) {
    fun toggleFollowing(user: Principal, otherUser: String) {
        db.useInstance { conn ->
            followerDao.toggleFollow(conn, user, otherUser)
        }
    }

    fun viewFollowers(username: String, pagination: PaginationRequest): List<String> {
        return db.useInstance { conn ->
            followerDao.viewFollowers(conn, username, pagination)
        }
    }

    fun viewFollowing(username: String, pagination: PaginationRequest): List<String> {
        return db.useInstance { conn ->
            followerDao.viewFollowing(conn, username, pagination)
        }
    }

    fun viewSnacker(username: String): Snacker {
        return db.useInstance { conn ->
            val tags = userTagDao.listTagsForUser(conn, username)
            val followerCount = followerDao.countFollowers(conn, username)
            Snacker(username, followerCount.toInt(), tags)
        }
    }
}
