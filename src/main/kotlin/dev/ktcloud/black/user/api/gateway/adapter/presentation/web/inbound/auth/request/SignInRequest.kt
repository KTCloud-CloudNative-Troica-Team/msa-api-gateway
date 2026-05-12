package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.request

data class SignInRequest(
    val email: String,
    val password: String,
)
