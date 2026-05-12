package dev.ktcloud.black.user.api.gateway.application.order.port.inbound

import dev.ktcloud.black.user.api.gateway.application.order.dto.OrderLineItemDto

interface FetchOrderQuery {
    suspend fun fetchOrder(query: In): Out

    data class In(
        val id: Long
    )

    data class Out(
        val id: Long,
        val status: String,
        val orderLineItems: List<OrderLineItemDto>,
    )
}