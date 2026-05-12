package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.product

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductsResponse

interface UserProductApiGatewayRestController {
    suspend fun fetchProduct(id: String): FetchProductResponse
    suspend fun fetchProducts(): FetchProductsResponse
}