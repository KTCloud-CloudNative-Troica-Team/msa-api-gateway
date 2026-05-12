package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.inventory

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoriesResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoryResponse

interface UserInventoryApiGatewayRestController {
    suspend fun fetchInventory(id: Long): FetchInventoryResponse
    suspend fun fetchInventories(): FetchInventoriesResponse
}