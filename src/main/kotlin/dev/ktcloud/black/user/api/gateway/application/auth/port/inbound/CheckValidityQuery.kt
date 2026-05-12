package dev.ktcloud.black.user.api.gateway.application.auth.port.inbound

import dev.ktcloud.black.user.api.gateway.application.auth.dto.UserDto

interface CheckValidityQuery {
    suspend fun checkValidity(query: In): Out

    data class In(
        val accessToken: String,
    )

    data class Out(
        val user: UserDto
    )
}