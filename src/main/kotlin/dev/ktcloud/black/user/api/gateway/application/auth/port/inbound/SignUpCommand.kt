package dev.ktcloud.black.user.api.gateway.application.auth.port.inbound

interface SignUpCommand {
    suspend fun signUp(command: In)

    data class In(
        val email: String,
        val plainPassword: String,
        val name: String
    )
}