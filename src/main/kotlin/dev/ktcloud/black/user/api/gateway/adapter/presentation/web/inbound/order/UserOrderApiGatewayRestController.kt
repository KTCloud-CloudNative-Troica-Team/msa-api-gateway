package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.request.CreateOrderRequest
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.response.CreateOrderResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrderResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.response.FetchOrdersResponse

interface UserOrderApiGatewayRestController {
    suspend fun createOrder(request: CreateOrderRequest): CreateOrderResponse
    suspend fun fetchOrders(): FetchOrdersResponse
    suspend fun fetchOrder(id: Long): FetchOrderResponse
}