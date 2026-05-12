package dev.ktcloud.black.user.api.gateway.application.order.dto

data class OrderLineItemDto(
    val inventoryId: Long,
    val productId: String,
    val skuCode: String,
    val price: Int,
    val quantity: Int,
    val status: String,
)
