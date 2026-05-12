package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.order.response

import dev.ktcloud.black.user.api.gateway.application.order.dto.OrderDto

data class FetchOrdersResponse(
    val orders: List<OrderDto>
)