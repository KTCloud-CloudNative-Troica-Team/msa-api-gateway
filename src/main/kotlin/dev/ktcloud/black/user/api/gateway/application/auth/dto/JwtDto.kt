package dev.ktcloud.black.user.api.gateway.application.auth.dto

data class JwtDto(
    val accessToken: String,
    val refreshToken: String,
)
