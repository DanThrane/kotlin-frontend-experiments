package dk.thrane.playground.site.auth

import dk.thrane.playground.MigrationHandler
import dk.thrane.playground.psql.SQLTable
import kotlinx.serialization.Serializable

@Serializable
data class TokenTable(
    val username: String,
    val token: String,
    val expiry: Long
) {
    override fun toString() = "TokenTable($username, $expiry)"

    companion object : SQLTable("tokens") {
        override fun registerMigrations(handler: MigrationHandler) {
            handler.addScript("tokens init") { conn ->
                conn.sendCommand(
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
}
