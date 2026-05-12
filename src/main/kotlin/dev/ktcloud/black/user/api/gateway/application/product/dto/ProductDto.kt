package dev.ktcloud.black.user.api.gateway.application.product.dto

data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Int
)