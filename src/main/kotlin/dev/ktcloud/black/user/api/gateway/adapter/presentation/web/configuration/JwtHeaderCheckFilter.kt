package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.CheckValidityRequest
import dev.ktcloud.black.user.api.gateway.application.auth.dto.UserDto
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import reactor.core.publisher.Mono

@Component
class JwtHeaderCheckFilter(
    @GrpcClient("auth-service")
    private val authServiceStub: AuthServiceGrpcKt.AuthServiceCoroutineStub
): WebFilter {
    companion object {
        val MATCH_LIST = listOf("/api/v1/orders")

        fun checkMatchPath(path: String): Boolean = MATCH_LIST.any { path.startsWith(it) }
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!checkMatchPath(exchange.request.uri.path)) return chain.filter(exchange)

        return mono {
            val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                chain.filter(exchange).awaitSingleOrNull()

                return@mono null
            }

            val token = authHeader.substring(7)

            val response = authServiceStub.checkValidity(
                CheckValidityRequest.newBuilder()
                    .setAccessToken(token)
                    .build()
            )

            val userDto = UserDto(
                id = response.id,
                role = response.role,
                email = response.email,
                name = response.name,
            )

            val authorities = listOf(SimpleGrantedAuthority(response.role))

            val auth = UsernamePasswordAuthenticationToken(
                userDto,
                null,
                authorities
            )

            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .awaitSingle()
        }
    }
}