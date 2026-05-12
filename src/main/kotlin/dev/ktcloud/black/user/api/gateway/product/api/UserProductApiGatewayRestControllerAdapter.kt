package dev.ktcloud.black.user.api.gateway.product.api

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.product.UserProductApiGatewayRestController
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.product.response.FetchProductsResponse
import dev.ktcloud.black.user.api.gateway.application.product.dto.ProductDto
import dev.ktcloud.black.user.api.gateway.application.product.port.inbound.FetchProductQuery
import dev.ktcloud.black.user.api.gateway.application.product.port.inbound.FetchProductsQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/products")
class UserProductApiGatewayRestControllerAdapter(
    private val fetchProductQuery: FetchProductQuery,
    private val fetchProductsQuery: FetchProductsQuery
): UserProductApiGatewayRestController {
    @Operation(summary = "상품 조회")
    @ApiResponse(responseCode = "200", description = "상품 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("{id}")
    override suspend fun fetchProduct(@PathVariable id: String): FetchProductResponse {
        val result = fetchProductQuery.fetchProduct(
            FetchProductQuery.In(id)
        )

        return FetchProductResponse(
            product = ProductDto(
                id = result.id,
                name = result.name,
                description = result.description,
                price = result.price,
            )
        )
    }

    @Operation(summary = "상품 전체 조회")
    @ApiResponse(responseCode = "201", description = "상품 전체 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping
    override suspend fun fetchProducts(): FetchProductsResponse {
        val result = fetchProductsQuery.fetchProducts()

        return FetchProductsResponse(
            products = result.map {
                ProductDto(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    price = it.price,
                )
            }
        )
    }
}