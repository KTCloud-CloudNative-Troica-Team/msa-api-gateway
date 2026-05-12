package dev.ktcloud.black.user.api.gateway.application.inventory.service

import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.FetchInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryServiceGrpcKt
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoriesQuery
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoryQuery
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class InventoryQueryService(
    @GrpcClient("inventory-service")
    private val inventoryServiceStub: InventoryServiceGrpcKt.InventoryServiceCoroutineStub
): FetchInventoryQuery, FetchInventoriesQuery {
    override suspend fun fetchInventory(query: FetchInventoryQuery.In): FetchInventoryQuery.Out {
        val inventoryResponseDto = inventoryServiceStub.fetchInventory(
                FetchInventoryRequest.newBuilder()
                    .setId(query.id)
                    .build(),
            )

        return FetchInventoryQuery.Out(
            id = inventoryResponseDto.id,
            productId = inventoryResponseDto.productId,
            skuCode = inventoryResponseDto.skuCode,
            quantity = inventoryResponseDto.quantity,
        )
    }

    override suspend fun fetchAll(): List<FetchInventoriesQuery.Out> {
        val inventoryResponseDtos = inventoryServiceStub.fetchInventories(Empty.getDefaultInstance())

        return inventoryResponseDtos.inventoriesList.map {
            FetchInventoriesQuery.Out(
                id = it.id,
                productId = it.productId,
                skuCode = it.skuCode,
                quantity = it.quantity,
            )
        }
    }
}