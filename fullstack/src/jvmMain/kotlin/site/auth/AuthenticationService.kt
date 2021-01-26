package dk.thrane.playground.site.auth

import dk.thrane.playground.*
import dk.thrane.playground.psql.*
import dk.thrane.playground.site.api.JWTClaims
import dk.thrane.playground.site.api.Principal
import dk.thrane.playground.site.api.PrincipalRole
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.Serializable
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
    private val db: PostgresConnectionPool,
    private val jwt: JWT,
    private val jwtAlgorithmAndKey: JWTAlgorithmAndKey
) {
    /*
    private val tokenCache = HashMap<String, CachedToken>()
    private val insertPrincipal = createInsertStatement(PrincipalTable, PrincipalTable.serializer()).asCommand()
    private val insertToken = createInsertStatement(TokenTable, TokenTable.serializer()).asCommand()

    @Serializable private data class FindByUsername(val username: String)
    private val findPrincipalByUsername = PreparedStatement(
        "select * from $PrincipalTable where username = ?username",
        FindByUsername.serializer(),
        PrincipalTable.serializer()
    ).asQuery()

    @Serializable private data class DeleteByToken(val token: String)
    private val deleteByToken = PreparedStatement(
        "delete from $TokenTable where token = ?token",
        DeleteByToken.serializer(),
        EmptyTable.serializer()
    ).asCommand()

    @Serializable private data class FindPrincipalByToken(val token: String, val now: Long)
    private val findPrincipalByToken = PreparedStatement(
        """
           select P.* 
           from $PrincipalTable P, $TokenTable T 
           where 
               P.username = T.username and
               token = ?token and 
               expiry > ?now 
        """,
        FindPrincipalByToken.serializer(),
        PrincipalTable.serializer()
    ).asQuery()
    */

    suspend fun createUser(
        role: PrincipalRole,
        username: String,
        password: String
    ) {
        TODO()
        /*
        val hashedPassword = hashPassword(password.toCharArray())
        db.useTransaction { conn ->
            insertPrincipal(conn,
                PrincipalTable(
                    username,
                    role,
                    hashedPassword.password,
                    hashedPassword.salt
                )
            )
        }
        */
    }

    suspend fun login(username: String, password: String): LoginResponse? {
        TODO()
        /*
        db.useTransaction { conn ->
            val principal = findPrincipalByUsername(conn,
                FindByUsername(username)
            ).singleOrNull() ?: run {
                // Always run password hashing.
                // This avoid leaking information about the user's existence through timing attacks.
                hashPassword(password.toCharArray())
                return null
            }

            if (!principal.password.contentEquals(hashPassword(password.toCharArray(), principal.salt).password)) {
                return null
            }

            val token = TokenTable(
                username,
                createLoginToken(),
                System.currentTimeMillis() + tokenExpiryTime
            )
            insertToken(conn, token)
            return LoginResponse(
                Principal(
                    principal.username,
                    principal.role
                ), token.token
            )
        }
        */
    }

    suspend fun logout(token: String) {
        TODO()
        /*
        db.useTransaction { conn ->
            deleteByToken(conn,
                DeleteByToken(token)
            )
        }
        */
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

        val claims = Json.Default.decodeFromJsonElement(JWTClaims.serializer(), decodedToken.body)
        if (System.currentTimeMillis() > claims.exp) return null

        return Principal(claims.sub, claims.role)
    }

    suspend fun validateRefreshToken(token: String?): Principal? {
        TODO()
        /*
        if (token == null) return null

        val cachedToken = tokenCache[token]
        if (cachedToken != null && cachedToken.expiry >= System.currentTimeMillis()) {
            return cachedToken.principal
        }

        db.useTransaction { conn ->
            val principal = findPrincipalByToken(
                conn,
                FindPrincipalByToken(
                    token,
                    System.currentTimeMillis()
                )
            ).singleOrNull() ?: return null

            val mappedPrincipal = Principal(principal.username, principal.role)
            cacheToken(token, mappedPrincipal)
            return mappedPrincipal
        }
        */
    }

    private fun cacheToken(token: String, mappedPrincipal: Principal) {
        TODO()
        /*
        synchronized(tokenCache) {
            tokenCache[token] = CachedToken(
                System.currentTimeMillis() + cacheExpiryTime,
                mappedPrincipal
            )
        }
        */
    }

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
        private val log = Log("AuthenticationService")
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
    /*
    val capturedToken = jwtToken ?: throw RPCException(ResponseCode.UNAUTHORIZED, "Unauthorized")
    val principal = validateJWT(capturedToken) ?: throw RPCException(ResponseCode.UNAUTHORIZED, "Unauthorized")
    if (principal.role !in validRoles) {
        throw RPCException(ResponseCode.FORBIDDEN, "Forbidden")
    }
    return principal
     */
    TODO()
}
