package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

/**
 * R-50 (평가 기본 (2)-5): Spring Cloud Gateway RequestRateLimiter용 KeyResolver.
 *
 * SC Gateway의 RequestRateLimiter filter는 "어떤 단위로 카운트할지"를 KeyResolver Bean
 * 으로 결정함. 본 프로젝트는 사용자별 제한이 목표이므로:
 *   1) 인증된 요청: Authorization 헤더의 Bearer 토큰 자체를 key로 사용함
 *      (JWT 본문 parse 없이도 동일 토큰=동일 사용자 보장됨)
 *   2) 익명 요청: client IP를 fallback key로 사용함
 *   3) 둘 다 없으면 "anonymous" 단일 버킷에 묶임 (의도된 broad-fence)
 *
 * 실 운영 시 JWT subject(sub) parse 로 변경하면 정확도가 더 높음. 발표 단계에서는
 * 토큰 자체 hash로도 같은 효과를 냄.
 *
 * Redis Token Bucket 알고리즘은 application.yaml의 redis-rate-limiter.* 설정으로 조정함.
 */
@Configuration
class RateLimitKeyResolverConfig {

    @Bean
    fun userKeyResolver(): KeyResolver = KeyResolver { exchange ->
        val authHeader = exchange.request.headers.getFirst("Authorization")
        val key = authHeader?.removePrefix("Bearer ")?.trim()?.ifBlank { null }
            ?: exchange.request.remoteAddress?.address?.hostAddress
            ?: "anonymous"
        Mono.just(key)
    }
}
