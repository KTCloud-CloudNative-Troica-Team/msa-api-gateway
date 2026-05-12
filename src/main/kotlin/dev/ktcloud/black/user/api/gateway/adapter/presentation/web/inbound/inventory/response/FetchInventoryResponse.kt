package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.inventory.response

import dev.ktcloud.black.user.api.gateway.application.inventory.dto.InventoryDto

data class FetchInventoryResponse(
    val inventory: InventoryDto
)