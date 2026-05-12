package dev.ktcloud.black.user.api.gateway.application.product.port.inbound

interface FetchProductsQuery {
    suspend fun fetchProducts(): List<Out>

    data class Out(
        val id: String,
        val name: String,
        val description: String,
        val price: Int
    )
}