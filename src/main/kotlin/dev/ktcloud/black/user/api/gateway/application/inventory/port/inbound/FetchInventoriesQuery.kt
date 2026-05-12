package dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound

interface FetchInventoriesQuery {
    suspend fun fetchAll(): List<Out>

    data class Out(
        val id: Long,
        val productId: String,
        val skuCode: String,
        val quantity: Int,
    )
}