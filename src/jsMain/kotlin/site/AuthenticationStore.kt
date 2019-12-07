package dk.thrane.playground.site

import dk.thrane.playground.EmptyOutgoingMessage
import dk.thrane.playground.call
import dk.thrane.playground.components.BoundData
import dk.thrane.playground.components.LocalStorage
import dk.thrane.playground.site.api.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object AuthenticationStore {
    private val mutableToken = BoundData<String?>(null)
    val token = mutableToken.asImmutable()

    private val mutablePrincipal = BoundData<Principal?>(null)
    val principal = mutablePrincipal.asImmutable()

    // Note: You really shouldn't do something like this as it can easily be stolen by a successful XSS attack.
    private var unsafeAccessToken by LocalStorage.delegate()

    suspend fun login(username: String, password: String) {
        val resp = Authentication.login.call(
            connectionPool,
            LoginRequest(username, password)
        )
        mutableToken.currentValue = resp[LoginResponse.token]
    }

    suspend fun logout() {
        val capturedToken = token.currentValue
        if (capturedToken != null) {
            Authentication.logout.call(
                connectionPool,
                LogoutRequest(capturedToken)
            )
            mutableToken.currentValue = null
        }
    }

    init {
        mutableToken.currentValue = unsafeAccessToken

        token.addHandler { newToken ->
            unsafeAccessToken = newToken

            if (newToken == null) {
                mutablePrincipal.currentValue = null
            } else {
                // Note: We might at a later point get a better scope. But for the moment this authentication state
                // lives for the entire duration of the application.
                GlobalScope.launch {
                    val resp = Authentication.whoami.call(
                        connectionPool,
                        EmptyOutgoingMessage(),
                        auth = token.currentValue
                    )
                    mutablePrincipal.currentValue = resp.toModel()
                }
            }
        }
    }
}
