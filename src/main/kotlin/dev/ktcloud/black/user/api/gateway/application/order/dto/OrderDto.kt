package dev.ktcloud.black.user.api.gateway.application.order.dto

data class OrderDto(
    val id: Long,
    val status: String,
    val orderLineItems: List<OrderLineItemDto>,
)