package dev.ktcloud.black.user.api.gateway.application.product.port.inbound

interface FetchProductQuery {
    suspend fun fetchProduct(query: In): Out

    data class In(val id: String)

    data class Out(
        val id: String,
        val name: String,
        val description: String,
        val price: Int
    )
}