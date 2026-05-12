package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.product.response

import dev.ktcloud.black.user.api.gateway.application.product.dto.ProductDto

data class FetchProductsResponse(
    val products: List<ProductDto>
)
