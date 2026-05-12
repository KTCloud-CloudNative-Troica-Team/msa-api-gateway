package dev.ktcloud.black.user.api.gateway.application.order.port.inbound

import dev.ktcloud.black.user.api.gateway.application.order.dto.OrderLineItemDto

interface FetchOrdersQuery {
    suspend fun fetchOrders(): List<Out>

    data class Out(
        val id: Long,
        val status: String,
        val orderLineItems: List<OrderLineItemDto>,
    )
}