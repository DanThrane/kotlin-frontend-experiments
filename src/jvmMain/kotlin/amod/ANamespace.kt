package dk.thrane.playground.amod

import dk.thrane.playground.RPCNamespace
import kotlinx.serialization.internal.UnitSerializer

object ANamespace : RPCNamespace("a") {
    val foo by call(UnitSerializer, UnitSerializer)
}
