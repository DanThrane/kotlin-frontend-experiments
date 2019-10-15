package dk.thrane.playground.site.service

import dk.thrane.playground.*
import dk.thrane.playground.site.api.PaginationRequest
import dk.thrane.playground.site.api.Principal
import dk.thrane.playground.site.api.offset
import java.sql.Connection

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
            ).executeUpdate()
        }
    }
}

class FollowerDao {
    fun viewFollowers(conn: Connection, user: String, pagination: PaginationRequest): List<String> {
        return conn
            .statement(
                """
                    select * 
                    from $SnackerFollowers
                    where ${SnackerFollowers.username} = ?username
                    offset ?off limit ?lim
                """.trimIndent(),

                mapOf(
                    "username" to user,
                    "off" to pagination.offset,
                    "lim" to pagination.itemsPerPage
                )
            )
            .mapQuery { it.mapTable(SnackerFollowers)[SnackerFollowers.follower] }
    }

    fun countFollowers(conn: Connection, user: String): Long {
        return conn
            .prepareStatement("select count(*) from $SnackerFollowers where ${SnackerFollowers.username} = ?")
            .apply {
                setString(1, user)
            }
            .mapQuery { it.getLong(1) }
            .single()
    }

    fun viewFollowing(conn: Connection, user: String, pagination: PaginationRequest): List<String> {
        return conn
            .statement(
                """
                    select * from $SnackerFollowers
                    where ${SnackerFollowers.follower} = ?follower
                    offset ?off limit ?lim
                """.trimIndent(),

                mapOf(
                    "follower" to user,
                    "off" to pagination.offset,
                    "lim" to pagination.itemsPerPage
                )
            )
            .mapQuery { it.mapTable(SnackerFollowers)[SnackerFollowers.follower] }
    }

    fun countFollowing(conn: Connection, user: String): Long {
        return conn
            .prepareStatement("select count(*) from $SnackerFollowers where ${SnackerFollowers.follower} = ?")
            .apply {
                setString(1, user)
            }
            .mapQuery { it.getLong(1) }
            .single()
    }

    fun toggleFollow(conn: Connection, user: Principal, otherUser: String) {
        val deletedRows = conn
            .statement(
                """
                    delete from $SnackerFollowers 
                    where 
                        ${SnackerFollowers.username} = ?username and 
                        ${SnackerFollowers.follower} = ?follower
                """.trimIndent(),
                mapOf("username" to user.username, "follower" to otherUser)
            )
            .executeUpdate()

        if (deletedRows == 0) {
            conn.insert(SnackerFollowers, listOf(SQLRow().also { row ->
                row[SnackerFollowers.username] = user.username
                row[SnackerFollowers.follower] = otherUser
            }))
        }
    }
}
