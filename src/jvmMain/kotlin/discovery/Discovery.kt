package dk.thrane.playground.discovery

import dk.thrane.playground.EmptyMessage
import dk.thrane.playground.RPCNamespace
import kotlinx.serialization.Serializable

@Serializable
data class ServiceLocation(val hostname: String, val port: Int, val namespaces: List<String>)

@Serializable
data class ServiceQuery(val namespace: String)

@Serializable
data class ServiceQueryResponse(val locations: List<ServiceLocation>)

object Discovery : RPCNamespace("discovery") {
    val register by call(ServiceLocation.serializer(), EmptyMessage.serializer())
    val ping by call(ServiceLocation.serializer(), EmptyMessage.serializer())
    val destroy by call(ServiceLocation.serializer(), EmptyMessage.serializer())
    val search by call(ServiceQuery.serializer(), ServiceQueryResponse.serializer())
}
