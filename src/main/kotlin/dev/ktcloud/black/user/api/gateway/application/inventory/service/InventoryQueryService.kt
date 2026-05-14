package dev.ktcloud.black.user.api.gateway.application.inventory.service

import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.FetchInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryServiceGrpcKt
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoriesQuery
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoryQuery
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import net.devh.boot.grpc.client.inject.GrpcClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * R-41 (평가 기본 (2)-3, (2)-4): inventory-service 장애 격리 + Response Aggregate.
 *
 * Circuit Breaker가 OPEN 상태이거나 gRPC 호출 자체가 실패하면 fallback 응답을 반환함.
 * - fetchInventory: quantity=-1 + skuCode="UNAVAILABLE" sentinel — 클라이언트는 이 값으로
 *   "재고 정보 일시 사용 불가"를 인지함.
 * - fetchAll: emptyList() — 빈 카탈로그 응답 (다른 도메인 정보는 정상 제공 가능).
 *
 * Resilience4j Kotlin 확장 `executeSuspendFunction`은 coroutine context를 보존하면서
 * Circuit Breaker 상태 전이를 적용함. annotation 방식보다 명시적이라 fallback 의도가
 * 코드에 직접 드러남.
 */
@Service
class InventoryQueryService(
    @GrpcClient("inventory-service")
    private val inventoryServiceStub: InventoryServiceGrpcKt.InventoryServiceCoroutineStub,
    circuitBreakerRegistry: CircuitBreakerRegistry,
) : FetchInventoryQuery, FetchInventoriesQuery {

    private val log = LoggerFactory.getLogger(javaClass)
    private val cb = circuitBreakerRegistry.circuitBreaker("inventory-service")

    override suspend fun fetchInventory(query: FetchInventoryQuery.In): FetchInventoryQuery.Out =
        runCatching {
            cb.executeSuspendFunction {
                val dto = inventoryServiceStub.fetchInventory(
                    FetchInventoryRequest.newBuilder().setId(query.id).build(),
                )
                FetchInventoryQuery.Out(
                    id = dto.id,
                    productId = dto.productId,
                    skuCode = dto.skuCode,
                    quantity = dto.quantity,
                )
            }
        }.getOrElse { ex ->
            log.warn("inventory-service unavailable — fallback partial response (id=${query.id}): ${ex.message}")
            FetchInventoryQuery.Out(
                id = query.id,
                productId = "",
                skuCode = "UNAVAILABLE",
                quantity = -1,
            )
        }

    override suspend fun fetchAll(): List<FetchInventoriesQuery.Out> =
        runCatching {
            cb.executeSuspendFunction {
                inventoryServiceStub.fetchInventories(Empty.getDefaultInstance())
                    .inventoriesList.map {
                        FetchInventoriesQuery.Out(
                            id = it.id,
                            productId = it.productId,
                            skuCode = it.skuCode,
                            quantity = it.quantity,
                        )
                    }
            }
        }.getOrElse { ex ->
            log.warn("inventory-service unavailable — fallback empty list: ${ex.message}")
            emptyList()
        }
}
