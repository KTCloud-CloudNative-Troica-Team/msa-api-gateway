package dev.ktcloud.black.user.api.gateway.application.inventory.dto

data class InventoryDto(
    val id: Long,
    val productId: String,
    val skuCode: String,
    val quantity: Int,
)
