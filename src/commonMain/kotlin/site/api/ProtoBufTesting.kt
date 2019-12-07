package dk.thrane.playground.site.api

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

@Serializable
data class KTestInt32(@SerialId(1) val a: Int)
