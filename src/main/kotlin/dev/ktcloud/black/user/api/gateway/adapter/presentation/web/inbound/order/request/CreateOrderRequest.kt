package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.request

data class CreateOrderRequest(
    val items: List<OrderLineItemRequest>
) {
    data class OrderLineItemRequest(
        val inventoryId: Long,
        val productId: String,
        val skuCode: String,
        val price: Int,
        val quantity: Int,
    )
}
