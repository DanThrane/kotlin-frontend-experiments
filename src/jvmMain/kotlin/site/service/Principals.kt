package dk.thrane.playground.site.service

import dk.thrane.playground.MigrationHandler
import dk.thrane.playground.db.SQLTable
import dk.thrane.playground.db.bytea
import dk.thrane.playground.db.varchar

object Principals : SQLTable("principals") {
    val username = varchar("username", 256)
    val role = varchar("role", 256)
    val password = bytea("password")
    val salt = bytea("salt")

    override fun migration(handler: MigrationHandler) {
        handler.addScript("principals init") { conn ->
            conn.sendPreparedStatement(
                """
                create table principals(
                    username varchar(256),
                    role varchar(256),
                    password bytea,
                    salt bytea,
                    
                    primary key (username)
                )
                """
            )
        }
    }
}
