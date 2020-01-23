package dk.thrane.playground.bmod

import dk.thrane.playground.RPCNamespace
import kotlinx.serialization.internal.UnitSerializer

object BNamespace : RPCNamespace("b") {
    val bar by call(UnitSerializer, UnitSerializer)
}
