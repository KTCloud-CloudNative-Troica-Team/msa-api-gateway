package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.response

import dev.ktcloud.black.user.api.gateway.application.auth.dto.JwtDto
import dev.ktcloud.black.user.api.gateway.application.auth.dto.UserDto

data class SignInResponse(
    val user: UserDto,
    val token: JwtDto
)
