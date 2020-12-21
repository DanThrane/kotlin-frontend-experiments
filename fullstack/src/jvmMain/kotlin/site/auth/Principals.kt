package dk.thrane.playground.site.auth

import dk.thrane.playground.MigrationHandler
import dk.thrane.playground.psql.SQLTable
import dk.thrane.playground.site.api.PrincipalRole
import kotlinx.serialization.Serializable

@Serializable
class PrincipalTable(
    val username: String,
    val role: PrincipalRole,
    val password: ByteArray,
    val salt: ByteArray
) {
    override fun toString() = "PrincipalTable($username, $role)"

    companion object : SQLTable("principals") {
        override fun registerMigrations(handler: MigrationHandler) {
            handler.addScript("principals init") { conn ->
                conn.sendCommand(
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
}
