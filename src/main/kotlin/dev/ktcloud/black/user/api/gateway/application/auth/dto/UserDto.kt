package dev.ktcloud.black.user.api.gateway.application.auth.dto

data class UserDto(
    val id: String,
    val role: String,
    val email: String,
    val name: String,
)
