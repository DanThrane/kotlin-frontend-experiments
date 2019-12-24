package dk.thrane.playground.site.service

import dk.thrane.playground.MigrationHandler
import dk.thrane.playground.db.SQLTable
import dk.thrane.playground.db.long
import dk.thrane.playground.db.varchar

object Tokens : SQLTable("tokens") {
    val username = varchar("username", 256)
    val token = varchar("token", 256)
    val expiry = long("expiry")

    override fun migration(handler: MigrationHandler) {
        handler.addScript("initial table") { conn ->
            conn.sendPreparedStatement(
                """
                create table tokens(
                    username varchar(256),
                    token varchar(256),
                    expiry bigint,
                    
                    primary key (token),
                    foreign key (username) references principals(username)
                )
                """
            )
        }
    }

}
