package dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound

interface FetchInventoryQuery {
    suspend fun fetchInventory(query: In): Out

    data class In(
        val id: Long
    )

    data class Out(
        val id: Long,
        val productId: String,
        val skuCode: String,
        val quantity: Int,
    )
}