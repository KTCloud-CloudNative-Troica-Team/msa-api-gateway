package dev.ktcloud.black.user.api.gateway.application.order.service

import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.FetchOrderRequest
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.OrderListItemResponseDto
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.OrderServiceGrpcKt
import dev.ktcloud.black.user.api.gateway.application.order.dto.OrderLineItemDto
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.FetchOrderQuery
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.FetchOrdersQuery
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class OrderQueryService(
    @GrpcClient("order-service")
    private val orderServiceStub: OrderServiceGrpcKt.OrderServiceCoroutineStub
): FetchOrderQuery, FetchOrdersQuery {
    private fun mapOrderLineItem(orderLineItem: OrderListItemResponseDto): OrderLineItemDto {
        return OrderLineItemDto(
            inventoryId = orderLineItem.inventoryId,
            productId = orderLineItem.productId,
            skuCode = orderLineItem.skuCode,
            price = orderLineItem.price,
            quantity = orderLineItem.quantity,
            status = orderLineItem.status,
        )
    }

    override suspend fun fetchOrder(query: FetchOrderQuery.In): FetchOrderQuery.Out {
        val response = orderServiceStub.fetchOrder(
            FetchOrderRequest.newBuilder()
                .setId(query.id)
                .build()
        )

        return FetchOrderQuery.Out(
            id = response.id,
            status = response.status,
            orderLineItems = response.orderLineItemsList.map(::mapOrderLineItem)
        )
    }

    override suspend fun fetchOrders(): List<FetchOrdersQuery.Out> {
        val response = orderServiceStub.fetchOrders(Empty.getDefaultInstance())

        return response.ordersList.map {
            FetchOrdersQuery.Out(
                id = it.id,
                status = it.status,
                orderLineItems = it.orderLineItemsList.map(::mapOrderLineItem)
            )
        }
    }
}