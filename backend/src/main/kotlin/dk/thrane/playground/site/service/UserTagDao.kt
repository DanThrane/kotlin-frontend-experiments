package dk.thrane.playground.site.service

import dk.thrane.playground.*
import dk.thrane.playground.site.api.SnackTag
import java.sql.Connection

object SnackerTags : SQLTable("snacker_tags") {
    val username = varchar("username", 256)
    val tag = varchar("tag", 256)

    override fun migration(handler: MigrationHandler) {
        handler.addScript("initial table") { conn ->
            conn.prepareStatement(
                """
                    create table snacker_tags(
                        username varchar(256),
                        tag varchar(256),
                        primary key(username, tag)
                    )
                """.trimIndent()
            ).executeUpdate()
        }
    }
}

class UserTagDao {
    fun listTagsForUser(conn: Connection, username: String): List<SnackTag> {
        return conn
            .statement(
                """
                    select *
                    from $SnackerTags
                    where ${SnackerTags.username} = ?username
                """.trimIndent(),
                mapOf("username" to username)
            )
            .mapQuery { row ->
                val name = row.mapTable(SnackerTags)[SnackerTags.tag]
                SnackTag.fromString(name)!!
            }
    }

    fun setTagsForUser(conn: Connection, username: String, tags: Set<SnackTag>) {
        conn
            .prepareStatement("delete from $SnackerTags where ${SnackerTags.username} = ?")
            .apply {
                setString(1, username)
            }
            .executeUpdate()

        conn.insert(SnackerTags, tags.map { tag ->
            SQLRow().also { row ->
                row[SnackerTags.username] = username
                row[SnackerTags.tag] = tag.name
            }
        })
    }
}
