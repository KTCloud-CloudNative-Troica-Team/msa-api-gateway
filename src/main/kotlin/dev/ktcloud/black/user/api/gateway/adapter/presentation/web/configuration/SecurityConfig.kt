package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtHeaderCheckFilter: JwtHeaderCheckFilter
) {
    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .addFilterAt(jwtHeaderCheckFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange { exchange ->
                exchange
                    .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                    .pathMatchers("/api/v1/orders/**").authenticated()
                    .anyExchange().permitAll()
            }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .build()
    }
}