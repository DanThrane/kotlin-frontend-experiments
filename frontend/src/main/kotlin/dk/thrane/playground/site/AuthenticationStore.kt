package dk.thrane.playground.site

import dk.thrane.playground.EmptyOutgoingMessage
import dk.thrane.playground.call
import dk.thrane.playground.components.BoundData
import dk.thrane.playground.components.LocalStorage
import dk.thrane.playground.site.api.*
import kotlin.js.Promise

object AuthenticationStore {
    private val mutableToken = BoundData<String?>(null)
    val token = mutableToken.asImmutable()

    private val mutablePrincipal = BoundData<Principal?>(null)
    val principal = mutablePrincipal.asImmutable()

    // Note: You really shouldn't do something like this as it can easily be stolen by a successful XSS attack.
    private var unsafeAccessToken by LocalStorage.delegate()

    fun login(username: String, password: String) {
        Authentication.login.call(
            connectionPool,
            LoginRequest(username, password)
        ).then { resp ->
            mutableToken.currentValue = resp[LoginResponse.token]
        }
    }

    fun logout(): Promise<Unit> {
        val capturedToken = token.currentValue
        if (capturedToken != null) {
            return Authentication.logout.call(
                connectionPool,
                LogoutRequest(capturedToken)
            ).then {
                mutableToken.currentValue = null
            }
        }

        return Promise.resolve(Unit)
    }

    init {
        mutableToken.currentValue = unsafeAccessToken

        token.addHandler { newToken ->
            unsafeAccessToken = newToken

            if (newToken == null) {
                mutablePrincipal.currentValue = null
            } else {
                Authentication.whoami.call(
                    connectionPool,
                    EmptyOutgoingMessage(),
                    auth = token.currentValue
                ).then { resp ->
                    mutablePrincipal.currentValue = resp.toModel()
                }
            }
        }
    }
}
