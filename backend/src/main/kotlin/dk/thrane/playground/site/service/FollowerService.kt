package dk.thrane.playground.site.service

import dk.thrane.playground.*
import dk.thrane.playground.site.api.PaginationRequest
import dk.thrane.playground.site.api.Principal
import dk.thrane.playground.site.api.offset

object SnackerFollowers : SQLTable("snacker_followers") {
    val username = varchar("username", 256)
    val follower = varchar("follower", 256)

    override fun migration(handler: MigrationHandler) {
        handler.addScript("initial follower table") { conn ->
            conn.prepareStatement(
                """
                    create table snacker_followers(
                        username varchar(256),
                        follower varchar(256),
                        primary key (username, follower),
                        foreign key (username) references ${Principals},
                        foreign key (follower) references $Principals
                    )
                """.trimIndent()
            )
        }
    }
}

class FollowerService(private val db: DBConnectionPool) {
    fun viewFollowers(user: Principal, pagination: PaginationRequest): List<String> {
        return db.useInstance { conn ->
            conn
                .statement(
                    """
                        select * 
                        from $SnackerFollowers
                        where ${SnackerFollowers.username} = ?username
                        offset ?off limit ?lim
                    """.trimIndent(),

                    mapOf(
                        "username" to user.username,
                        "off" to pagination.offset,
                        "lim" to pagination.itemsPerPage
                    )
                )
                .mapQuery { it.mapTable(SnackerFollowers)[SnackerFollowers.follower] }
        }
    }

    fun countFollowers(user: Principal): Long {
        return db.useInstance { conn ->
            conn
                .prepareStatement("select count(*) from $SnackerFollowers where ${SnackerFollowers.username} = ?")
                .apply {
                    setString(1, user.username)
                }
                .mapQuery { it.getLong(1) }
                .single()
        }
    }

    fun viewFollowing(user: Principal, pagination: PaginationRequest): List<String> {
        return db.useInstance { conn ->
            conn
                .statement(
                    """
                        select * from $SnackerFollowers
                        where ${SnackerFollowers.follower} = ?follower
                        offset ?off limit ?lim
                    """.trimIndent(),

                    mapOf(
                        "follower" to user.username,
                        "off" to pagination.offset,
                        "lim" to pagination.itemsPerPage
                    )
                )
                .mapQuery { it.mapTable(SnackerFollowers)[SnackerFollowers.follower] }
        }
    }

    fun countFollowing(user: Principal): Long {
        return db.useInstance { conn ->
            conn
                .prepareStatement("select count(*) from $SnackerFollowers where ${SnackerFollowers.follower} = ?")
                .apply {
                    setString(1, user.username)
                }
                .mapQuery { it.getLong(1) }
                .single()
        }
    }

    fun follow(user: Principal, otherUser: String) {
        db.useInstance { conn ->
            conn.insert(SnackerFollowers, listOf(SQLRow().also { row ->
                row[SnackerFollowers.username] = user.username
                row[SnackerFollowers.follower] = otherUser
            }))
        }
    }
}
