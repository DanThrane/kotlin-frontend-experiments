package dk.thrane.playground.site.service

import dk.thrane.playground.*
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.sql.rowset.serial.SerialBlob

enum class PrincipalRole {
    USER,
    ADMIN
}

data class Principal(
    val username: String,
    val role: PrincipalRole
)

object Principals : SQLTable("principals") {
    val username = varchar("username", 256)
    val role = varchar("role", 256)
    val password = blob("password")

    val salt = blob("salt")
    override fun migration(handler: MigrationHandler) {
        handler.addScript("principals init") { conn ->
            conn.prepareStatement(
                """
                create table principals(
                    username varchar(256),
                    role varchar(256),
                    password blob,
                    salt blob,
                    
                    primary key (username)
                )
            """.trimIndent()
            ).executeUpdate()
        }
    }

}
object Tokens : SQLTable("tokens") {
    val username = Principals.username
    val token = varchar("token", 256)

    val expiry = long("expiry")
    override fun migration(handler: MigrationHandler) {
        handler.addScript("initial table") { conn ->
            conn.prepareStatement(
                """
                create table tokens(
                    username varchar(256),
                    token varchar(256),
                    expiry bigint,
                    
                    primary key (token),
                    foreign key (username) references principals(username)
                )
            """.trimIndent()
            ).executeUpdate()
        }
    }

}

data class HashedPasswordAndSalt(val password: ByteArray, val salt: ByteArray)

data class LoginResponse(val principal: Principal, val token: String)

class AuthenticationService(
    private val db: ConnectionPool
) {
    fun createUser(username: String, role: PrincipalRole, password: String) {
        db.useInstance { conn ->
            val hashedPassword = hashPassword(password.toCharArray())
            conn.insert(Principals, listOf(SQLRow().also { row ->
                row[Principals.username] = username
                row[Principals.password] = SerialBlob(hashedPassword.password)
                row[Principals.salt] = SerialBlob(hashedPassword.salt)
                row[Principals.role] = role.name
            }))
        }
    }

    fun login(username: String, password: String): LoginResponse? {
        db.useInstance { conn ->
            val principal = conn
                .prepareStatement(
                    """
                        select * from $Principals where ${Principals.username} = ?
                    """.trimIndent()
                )
                .apply {
                    setString(1, username)
                }
                .mapQuery { it.mapTable(Principals) }
                .singleOrNull() ?: return null

            val realPassword = principal[Principals.password].binaryStream.readBytes()
            val salt = principal[Principals.salt].binaryStream.readBytes()

            return if (realPassword.contentEquals(hashPassword(password.toCharArray(), salt).password)) {
                val token = createLoginToken()
                conn.insert(Tokens, listOf(SQLRow().also { row ->
                    row[Tokens.username] = username
                    row[Tokens.token] = token
                    row[Tokens.expiry] = System.currentTimeMillis() + tokenExpiryTime
                }))

                LoginResponse(principalFromRow(principal), token)
            } else {
                null
            }
        }
    }

    fun logout(token: String) {
        db.useInstance { conn ->
            conn.prepareStatement("delete from $Tokens where ${Tokens.token} = ?").apply {
                setString(1, token)
            }.executeUpdate()
        }
    }

    fun validateToken(token: String?): Principal? {
        if (token == null) return null

        db.useInstance { conn ->
            val row = conn
                .prepareStatement(
                    """
                    select P.* 
                    from $Principals P join $Tokens T 
                    where ${Tokens.token} = ? and ${Tokens.expiry} < ?
                """.trimIndent()
                )
                .apply {
                    setString(1, token)
                    setLong(2, System.currentTimeMillis())
                }
                .mapQuery { it.mapTable(Principals) }
                .singleOrNull() ?: return null

            return principalFromRow(row)
        }
    }

    private fun principalFromRow(row: SQLRow): Principal =
        Principal(
            row[Principals.username],
            PrincipalRole.valueOf(row[Principals.role])
        )

    private fun createLoginToken(): String {
        val bytes = ByteArray(tokenLength).also { secureRandom.nextBytes(it) }
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun hashPassword(password: CharArray, salt: ByteArray = genSalt()): HashedPasswordAndSalt {
        try {
            val skf = SecretKeyFactory.getInstance(keyFactory)
            val spec = PBEKeySpec(password, salt,
                iterations,
                keyLength
            )
            val key = skf.generateSecret(spec)
            Arrays.fill(password, '0')
            return HashedPasswordAndSalt(key.encoded, salt)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            throw RuntimeException(e)
        }
    }

    private fun genSalt(): ByteArray = ByteArray(saltLength).also { secureRandom.nextBytes(it) }

    companion object {
        private val secureRandom = SecureRandom()
        private const val keyFactory = "PBKDF2WithHmacSHA512"
        private const val iterations = 10000
        private const val saltLength = 16
        private const val tokenLength = 64
        private const val keyLength = 256
        private const val tokenExpiryTime = 1000L * 60 * 60 * 24 * 30
    }
}
