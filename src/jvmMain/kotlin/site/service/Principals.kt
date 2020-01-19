package dk.thrane.playground.site.service

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
        const val username = "username"
        const val role = "role"
        const val password = "password"
        const val salt = "salt"

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
