package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.request

data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String
)
