package dev.ktcloud.black.user.api.gateway.application.inventory.service

import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.Empty
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.FetchInventoriesResponse
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.FetchInventoryRequest
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.FetchInventoryResponse
import dev.ktcloud.black.inventory.service.adapter.presentation.web.inbound.grpc.InventoryServiceGrpcKt
import dev.ktcloud.black.user.api.gateway.application.inventory.port.inbound.FetchInventoryQuery
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * R-57 (평가 기본 (3)-1) + R-41 (평가 기본 (2)-3, (2)-4) 검증.
 *
 * InventoryQueryService 의 Circuit Breaker + Response Aggregate Fallback 동작 단위 테스트.
 * gRPC stub 을 mock 으로 대체하여 정상 / 실패 경로 둘 다 검증.
 */
@DisplayName("InventoryQueryService - Circuit Breaker + Fallback")
class InventoryQueryServiceTest {

    private val stub = mockk<InventoryServiceGrpcKt.InventoryServiceCoroutineStub>()
    private val registry = CircuitBreakerRegistry.ofDefaults()
    private val service = InventoryQueryService(stub, registry)

    @Test
    @DisplayName("fetchInventory 정상 경로 — stub 응답을 Out 으로 mapping")
    fun `fetchInventory 정상 경로`() = runTest {
        val grpcResponse = FetchInventoryResponse.newBuilder()
            .setId(42L)
            .setProductId("P-42")
            .setSkuCode("SKU-42")
            .setQuantity(100)
            .build()

        coEvery { stub.fetchInventory(any<FetchInventoryRequest>()) } returns grpcResponse

        val out = service.fetchInventory(FetchInventoryQuery.In(id = 42L))

        assertThat(out.id).isEqualTo(42L)
        assertThat(out.productId).isEqualTo("P-42")
        assertThat(out.skuCode).isEqualTo("SKU-42")
        assertThat(out.quantity).isEqualTo(100)
    }

    @Test
    @DisplayName("fetchInventory 실패 경로 — stub 예외 발생 시 fallback (quantity=-1, skuCode=UNAVAILABLE)")
    fun `fetchInventory fallback`() = runTest {
        coEvery {
            stub.fetchInventory(any<FetchInventoryRequest>())
        } throws RuntimeException("inventory-service unavailable")

        val out = service.fetchInventory(FetchInventoryQuery.In(id = 999L))

        // Response Aggregate 패턴 — 핵심 응답은 살리고 비핵심은 placeholder
        assertThat(out.id).isEqualTo(999L)             // 입력 id 그대로 echo
        assertThat(out.productId).isEqualTo("")
        assertThat(out.skuCode).isEqualTo("UNAVAILABLE")
        assertThat(out.quantity).isEqualTo(-1)          // sentinel
    }

    @Test
    @DisplayName("fetchAll 정상 경로 — stub 응답 리스트를 Out 으로 mapping")
    fun `fetchAll 정상 경로`() = runTest {
        val inv1 = FetchInventoryResponse.newBuilder()
            .setId(1L).setProductId("P-1").setSkuCode("S-1").setQuantity(10).build()
        val inv2 = FetchInventoryResponse.newBuilder()
            .setId(2L).setProductId("P-2").setSkuCode("S-2").setQuantity(20).build()
        val response = FetchInventoriesResponse.newBuilder()
            .addAllInventories(listOf(inv1, inv2))
            .build()

        coEvery { stub.fetchInventories(any<Empty>()) } returns response

        val out = service.fetchAll()

        assertThat(out).hasSize(2)
        assertThat(out[0].id).isEqualTo(1L)
        assertThat(out[0].quantity).isEqualTo(10)
        assertThat(out[1].id).isEqualTo(2L)
        assertThat(out[1].quantity).isEqualTo(20)
    }

    @Test
    @DisplayName("fetchAll 실패 경로 — stub 예외 시 fallback emptyList")
    fun `fetchAll fallback`() = runTest {
        coEvery {
            stub.fetchInventories(any<Empty>())
        } throws RuntimeException("inventory-service unavailable")

        val out = service.fetchAll()

        // Response Aggregate — 빈 리스트로 부분 응답
        assertThat(out).isEmpty()
    }
}
