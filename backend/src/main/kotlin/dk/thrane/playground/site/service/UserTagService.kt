package dk.thrane.playground.site.service

import dk.thrane.playground.*
import dk.thrane.playground.site.api.SnackTag

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

class UserTagService(private val db: DBConnectionPool) {
    fun listTagsForUser(username: String): List<SnackTag> {
        return db.useInstance { conn ->
            conn
                .prepareStatement(
                    """
                        select *
                        from $SnackerTags
                        where ${SnackerTags.username} = ?
                    """.trimIndent()
                )
                .apply {
                    setString(1, username)
                }
                .mapQuery { row ->
                    SnackTag.fromString(
                        row.mapTable(SnackerTags)[SnackerTags.tag]
                    )
                }
                .filterNotNull()
        }
    }

    fun setTagsForUser(username: String, tags: Set<SnackTag>) {
        db.useInstance { conn ->
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
}
