package dk.thrane.playground.site.service

import dk.thrane.playground.JWT
import dk.thrane.playground.JWTAlgorithmAndKey
import dk.thrane.playground.RPCException
import dk.thrane.playground.ResponseCode
import dk.thrane.playground.db.DBConnectionPool
import dk.thrane.playground.db.SQLRow
import dk.thrane.playground.db.insert
import dk.thrane.playground.db.mapTable
import dk.thrane.playground.db.sendPreparedStatement
import dk.thrane.playground.db.useTransaction
import dk.thrane.playground.site.api.JWTClaims
import dk.thrane.playground.site.api.Principal
import dk.thrane.playground.site.api.PrincipalRole
import kotlinx.serialization.json.Json
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class HashedPasswordAndSalt(val password: ByteArray, val salt: ByteArray)

data class LoginResponse(val principal: Principal, val token: String)

private data class CachedToken(val expiry: Long, val principal: Principal)

class AuthenticationService(
    private val db: DBConnectionPool,
    private val jwt: JWT,
    private val jwtAlgorithmAndKey: JWTAlgorithmAndKey
) {
    private val tokenCache = HashMap<String, CachedToken>()

    suspend fun createUser(
        role: PrincipalRole,
        username: String,
        password: String
    ) {
        db.useTransaction { conn ->
            val hashedPassword = hashPassword(password.toCharArray())
            conn.insert(Principals, (SQLRow().also { row ->
                row[Principals.username] = username
                row[Principals.password] = hashedPassword.password
                row[Principals.salt] = hashedPassword.salt
                row[Principals.role] = role.name
            }))
        }
    }

    suspend fun login(username: String, password: String): LoginResponse? {
        db.useTransaction { conn ->
            val principal = conn
                .sendPreparedStatement(
                    {
                        setParameter("username", username)
                    },
                    """
                        select * from $Principals where ${Principals.username} = ?username
                    """.trimIndent()
                )
                .rows
                .mapTable(Principals)
                .singleOrNull() ?: return null

            val realPassword = principal[Principals.password]
            val salt = principal[Principals.salt]

            return if (realPassword.contentEquals(hashPassword(password.toCharArray(), salt).password)) {
                val token = createLoginToken()
                conn.insert(Tokens, SQLRow().also { row ->
                    row[Tokens.username] = username
                    row[Tokens.token] = token
                    row[Tokens.expiry] = System.currentTimeMillis() + tokenExpiryTime
                })

                val mappedPrincipal = principalFromRow(principal)
                cacheToken(token, mappedPrincipal)

                LoginResponse(mappedPrincipal, token)
            } else {
                null
            }
        }
    }

    suspend fun logout(token: String) {
        db.useTransaction { conn ->
            conn.sendPreparedStatement(
                { setParameter("token", token) },
                "delete from $Tokens where ${Tokens.token} = ?token"
            )
        }
    }

    suspend fun refresh(refreshToken: String): String {
        val principal = validateRefreshToken(refreshToken) ?: throw RPCException(ResponseCode.FORBIDDEN, "Invalid token")
        return jwt.create(
            jwtAlgorithmAndKey,
            JWTClaims(principal.username, System.currentTimeMillis() + (1_000 * 60 * 15), principal.role),
            JWTClaims.serializer()
        )
    }

    fun validateJWT(jwtToken: String): Principal? {
        val decodedToken = runCatching {
            jwt.verify(jwtToken, jwtAlgorithmAndKey)
        }.getOrNull() ?: return null

        val claims = Json.plain.fromJson(JWTClaims.serializer(), decodedToken.body)
        if (System.currentTimeMillis() > claims.exp) return null

        return Principal(claims.sub, claims.role)
    }

    suspend fun validateRefreshToken(token: String?): Principal? {
        if (token == null) return null

        val cachedToken = tokenCache[token]
        if (cachedToken != null && cachedToken.expiry >= System.currentTimeMillis()) {
            return cachedToken.principal
        }

        db.useTransaction { conn ->
            val row = conn
                .sendPreparedStatement(
                    {
                        setParameter("token", token)
                        setParameter("expiry", System.currentTimeMillis())
                    },
                    """
                    select P.* 
                    from $Principals P, $Tokens T 
                    where 
                        P.${Principals.username} = T.${Tokens.username} and
                        ${Tokens.token} = ?token and 
                        ${Tokens.expiry} > ?expiry
                    """
                )
                .rows
                .mapTable(Principals)
                .singleOrNull() ?: return null

            val mappedPrincipal = principalFromRow(row)
            cacheToken(token, mappedPrincipal)

            return mappedPrincipal
        }
    }

    private fun cacheToken(token: String, mappedPrincipal: Principal) {
        synchronized(tokenCache) {
            tokenCache[token] = CachedToken(System.currentTimeMillis() + cacheExpiryTime, mappedPrincipal)
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
            val spec = PBEKeySpec(
                password, salt,
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
        private const val cacheExpiryTime = 1000L * 60
    }
}

suspend fun AuthenticationService.verifyUser(
    jwtToken: String?,
    validRoles: Set<PrincipalRole> = setOf(PrincipalRole.USER, PrincipalRole.ADMIN)
): Principal {
    val capturedToken = jwtToken ?: throw RPCException(ResponseCode.UNAUTHORIZED, "Unauthorized")
    val principal = validateJWT(capturedToken) ?: throw RPCException(ResponseCode.UNAUTHORIZED, "Unauthorized")
    if (principal.role !in validRoles) {
        throw RPCException(ResponseCode.FORBIDDEN, "Forbidden")
    }
    return principal
}
