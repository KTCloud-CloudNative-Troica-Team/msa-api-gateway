package dev.ktcloud.black.user.api.gateway.application.auth.port.inbound

import dev.ktcloud.black.user.api.gateway.application.auth.dto.JwtDto
import dev.ktcloud.black.user.api.gateway.application.auth.dto.UserDto

interface SignInCommand {
    suspend fun signIn(command: In): Out

    data class In(
        val email: String,
        val plainPassword: String,
    )

    data class Out(
        val user: UserDto,
        val token: JwtDto
    )
}