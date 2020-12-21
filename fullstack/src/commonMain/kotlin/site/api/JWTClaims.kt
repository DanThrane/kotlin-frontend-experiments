package dk.thrane.playground.site.api

import kotlinx.serialization.Serializable

@Serializable
data class JWTClaims(
    val sub: String,
    val exp: Long,
    val role: PrincipalRole
)
