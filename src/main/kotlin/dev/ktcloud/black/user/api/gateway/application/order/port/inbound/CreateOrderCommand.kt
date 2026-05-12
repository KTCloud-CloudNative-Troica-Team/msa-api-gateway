package dev.ktcloud.black.user.api.gateway.application.order.port.inbound

import dev.ktcloud.black.user.api.gateway.application.order.dto.OrderLineItemDto

interface CreateOrderCommand {
    suspend fun createOrder(command: List<In>): Out

    data class In(
        val inventoryId: Long,
        val productId: String,
        val skuCode: String,
        val price: Int,
        val quantity: Int
    )

    data class Out(
        val id: Long,
        val status: String,
        val orderLineItems: List<OrderLineItemDto>,
    )
}