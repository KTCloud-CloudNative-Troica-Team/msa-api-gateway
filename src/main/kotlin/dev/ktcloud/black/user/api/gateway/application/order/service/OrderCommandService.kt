package dev.ktcloud.black.user.api.gateway.application.order.service

import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.CreateOrderRequest
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.CreateOrderRequestItem
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.OrderListItemResponseDto
import dev.ktcloud.black.order.service.adapter.presentation.web.inbound.grpc.OrderServiceGrpcKt
import dev.ktcloud.black.user.api.gateway.application.order.dto.OrderLineItemDto
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.CreateOrderCommand
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class OrderCommandService(
    @GrpcClient("order-service")
    private val orderServiceStub: OrderServiceGrpcKt.OrderServiceCoroutineStub
): CreateOrderCommand {
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

    override suspend fun createOrder(command: List<CreateOrderCommand.In>): CreateOrderCommand.Out {
        val createOrderRequestItems = command.map {
            CreateOrderRequestItem.newBuilder()
                .setInventoryId(it.inventoryId)
                .setProductId(it.productId)
                .setSkuCode(it.skuCode)
                .setPrice(it.price)
                .setQuantity(it.quantity)
                .build()
        }

        val createdResponse = orderServiceStub.createOrder(
            CreateOrderRequest.newBuilder()
                .addAllItems(createOrderRequestItems)
                .build()
        )

        return CreateOrderCommand.Out(
            id = createdResponse.id,
            status = createdResponse.status,
            orderLineItems = createdResponse.orderLineItemsList.map(::mapOrderLineItem)
        )
    }
}