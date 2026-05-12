package dev.ktcloud.black.user.api.gateway.inventory.api

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.inventory.UserInventoryApiGatewayRestController
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoriesResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.inventory.response.FetchInventoryResponse
import dev.ktcloud.black.user.api.gateway.application.inventory.dto.InventoryDto
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoriesQuery
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoryQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/inventories")
class UserInventoryApiGatewayRestControllerAdapter(
    private val fetchInventoryQuery: FetchInventoryQuery,
    private val fetchInventoriesQuery: FetchInventoriesQuery
): UserInventoryApiGatewayRestController {
    @Operation(summary = "재고 조회")
    @ApiResponse(responseCode = "200", description = "재고 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("{id}")
    override suspend fun fetchInventory(@PathVariable id: Long): FetchInventoryResponse {
        val result = fetchInventoryQuery.fetchInventory(
            FetchInventoryQuery.In(id)
        )

        return FetchInventoryResponse(
            InventoryDto(
                id = result.id,
                productId = result.productId,
                skuCode = result.skuCode,
                quantity = result.quantity,
            )
        )
    }

    @Operation(summary = "재고 전체 조회")
    @ApiResponse(responseCode = "200", description = "재고 전체 조회 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping
    override suspend fun fetchInventories(): FetchInventoriesResponse {
        val result = fetchInventoriesQuery.fetchAll()

        return FetchInventoriesResponse(
            inventories = result.map {
                InventoryDto(
                    id = it.id,
                    productId = it.productId,
                    skuCode = it.skuCode,
                    quantity = it.quantity,
                )
            }
        )
    }
}