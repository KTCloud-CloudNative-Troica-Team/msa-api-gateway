package dev.ktcloud.black.user.api.gateway.order.api

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.UserOrderApiGatewayRestController
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.request.CreateOrderRequest
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.response.CreateOrderResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrderResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrdersResponse
import dev.ktcloud.black.user.api.gateway.application.order.dto.OrderDto
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.CreateOrderCommand
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.FetchOrderQuery
import dev.ktcloud.black.user.api.gateway.application.order.port.inbound.FetchOrdersQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class UserOrderApiGatewayRestControllerAdapter(
    private val createOrderCommand: CreateOrderCommand,
    private val fetchOrderQuery: FetchOrderQuery,
    private val fetchOrdersQuery: FetchOrdersQuery
): UserOrderApiGatewayRestController {
    @Operation(summary = "주문 생성")
    @ApiResponse(responseCode = "201", description = "주문 생성 성공")
    @ResponseStatus(value = HttpStatus.CREATED)
    @PostMapping
    override suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse {
        val result = createOrderCommand.createOrder(
            command = request.items.map {
                CreateOrderCommand.In(
                    inventoryId = it.inventoryId,
                    productId = it.productId,
                    skuCode = it.skuCode,
                    price = it.price,
                    quantity = it.quantity,
                )
            }
        )

        return CreateOrderResponse(
            OrderDto(
                id = result.id,
                status = result.status,
                orderLineItems = result.orderLineItems
            )
        )
    }

    @Operation(summary = "주문 전체 조회")
    @ApiResponse(responseCode = "200", description = "주문 전체 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping
    override suspend fun fetchOrders(): FetchOrdersResponse {
        val result = fetchOrdersQuery.fetchOrders()

        return FetchOrdersResponse(
            orders = result.map { OrderDto(it.id, it.status, it.orderLineItems) }
        )
    }

    @Operation(summary = "주문 조회")
    @ApiResponse(responseCode = "200", description = "주문 조회 성공")
    @ResponseStatus(value = HttpStatus.CREATED)
    @GetMapping("{id}")
    override suspend fun fetchOrder(@PathVariable id: Long): FetchOrderResponse {
        val result = fetchOrderQuery.fetchOrder(
            FetchOrderQuery.In(id)
        )

        return FetchOrderResponse(
            order = OrderDto(
                id = result.id,
                status = result.status,
                orderLineItems = result.orderLineItems
            )
        )
    }

}