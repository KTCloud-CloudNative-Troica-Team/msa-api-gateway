package dev.ktcloud.black.user.api.gateway.application.product.service

import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.FetchProductRequest
import dev.ktcloud.black.product.service.adapter.presentation.web.inbound.grpc.ProductServiceGrpcKt
import dev.ktcloud.black.user.api.gateway.application.product.port.inbound.FetchProductQuery
import dev.ktcloud.black.user.api.gateway.application.product.port.inbound.FetchProductsQuery
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class ProductQueryService(
    @GrpcClient("product-service")
    private val productServiceStub: ProductServiceGrpcKt.ProductServiceCoroutineStub
): FetchProductQuery, FetchProductsQuery {
    override suspend fun fetchProduct(query: FetchProductQuery.In): FetchProductQuery.Out {
        val response = productServiceStub.fetchProduct(
            FetchProductRequest.newBuilder()
                .setId(query.id)
                .build()
        )

        return FetchProductQuery.Out(
            id = response.id,
            name = response.name,
            description = response.description,
            price = response.price,
        )
    }

    override suspend fun fetchProducts(): List<FetchProductsQuery.Out> {
        val response = productServiceStub.fetchAll(Empty.getDefaultInstance())

        return response.productsList.map {
            FetchProductsQuery.Out(
                id = it.id,
                name = it.name,
                description = it.description,
                price = it.price,
            )
        }
    }
}