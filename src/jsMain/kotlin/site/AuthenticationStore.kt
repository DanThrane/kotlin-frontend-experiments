package dk.thrane.playground.site

import dk.thrane.playground.EmptyMessage
import dk.thrane.playground.JWT
import dk.thrane.playground.RPCException
import dk.thrane.playground.ResponseCode
import dk.thrane.playground.call
import dk.thrane.playground.components.BoundData
import dk.thrane.playground.components.LocalStorage
import dk.thrane.playground.default
import dk.thrane.playground.site.api.Authentication
import dk.thrane.playground.site.api.JWTClaims
import dk.thrane.playground.site.api.LoginRequest
import dk.thrane.playground.site.api.LogoutRequest
import dk.thrane.playground.site.api.Principal
import dk.thrane.playground.site.api.RefreshRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.js.Date
import kotlin.time.measureTime

object AuthenticationStore {
    private val mutableRefreshToken = BoundData<String?>(null)
    val refreshToken = mutableRefreshToken.asImmutable()

    private val mutablePrincipal = BoundData<Principal?>(null)
    val principal = mutablePrincipal.asImmutable()

    private var accessToken by LocalStorage.delegate()

    // Note: You really shouldn't do something like this as it can easily be stolen by a successful XSS attack.
    private var unsafeRefreshToken by LocalStorage.delegate()

    suspend fun login(username: String, password: String) {
        val resp = Authentication.login.call(
            connectionPool,
            LoginRequest(username, password)
        )
        mutableRefreshToken.currentValue = resp.token
    }

    suspend fun logout() {
        val capturedToken = refreshToken.currentValue
        if (capturedToken != null) {
            Authentication.logout.call(
                connectionPool,
                LogoutRequest(capturedToken)
            )
            mutableRefreshToken.currentValue = null
        }
    }

    suspend fun getAccessTokenOrRefresh(): String {
        val currentAccessToken = accessToken ?: ""
        val currentJwt =
            runCatching { JWT.default.validate(currentAccessToken) }.getOrNull() ?: return refreshAccessToken()

        val claims = Json.plain.fromJson(JWTClaims.serializer(), currentJwt.header)
        if (claims.sub != principal.currentValue?.username) {
            mutableRefreshToken.currentValue = null
            unsafeRefreshToken = null
            accessToken = null
            throw RPCException(ResponseCode.FORBIDDEN, "Bad tokens")
        }

        if (claims.exp > Date().getTime().toLong()) {
            return refreshAccessToken()
        }

        return currentAccessToken
    }

    private suspend fun refreshAccessToken(): String {
        return Authentication.refresh
            .call(
                connectionPool,
                RefreshRequest(
                    refreshToken.currentValue ?: throw RPCException(
                        ResponseCode.UNAUTHORIZED,
                        "No active token"
                    )
                )
            )
            .token
            .also { token ->
                mutableRefreshToken.currentValue = token
            }
    }

    init {
        mutableRefreshToken.currentValue = unsafeRefreshToken

        refreshToken.addHandler { newToken ->
            unsafeRefreshToken = newToken

            if (newToken == null) {
                mutablePrincipal.currentValue = null
            } else {
                // Note: We might at a later point get a better scope. But for the moment this authentication state
                // lives for the entire duration of the application.
                GlobalScope.launch {
                    val resp = Authentication.whoami.call(
                        connectionPool,
                        EmptyMessage,
                        auth = refreshToken.currentValue
                    )
                    mutablePrincipal.currentValue = resp
                }
            }
        }
    }
}
