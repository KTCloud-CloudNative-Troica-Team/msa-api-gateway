package dev.ktcloud.black.user.api.gateway.application.auth.service

import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.AuthServiceGrpcKt
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignInRequest
import dev.ktcloud.black.auth.service.adapter.presentation.web.inbound.grpc.SignUpRequest
import dev.ktcloud.black.user.api.gateway.application.auth.dto.JwtDto
import dev.ktcloud.black.user.api.gateway.application.auth.dto.UserDto
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.SignInCommand
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.SignUpCommand
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

@Service
class AuthCommandService(
    @GrpcClient("auth-service")
    private val authServiceStub: AuthServiceGrpcKt.AuthServiceCoroutineStub
): SignUpCommand, SignInCommand {
    override suspend fun signUp(command: SignUpCommand.In) {
        authServiceStub.signUp(
            SignUpRequest.newBuilder()
                .setEmail(command.email)
                .setPlainPassword(command.plainPassword)
                .setName(command.name)
                .build()
        )
    }

    override suspend fun signIn(command: SignInCommand.In): SignInCommand.Out {
        val signInResponse = authServiceStub.signIn(
            SignInRequest.newBuilder()
                .setEmail(command.email)
                .setPlainPassword(command.plainPassword)
                .build()
        )

        return SignInCommand.Out(
            user = UserDto(
                id = signInResponse.user.id,
                role = signInResponse.user.role,
                email = signInResponse.user.email,
                name = signInResponse.user.name,
            ),
            token = JwtDto(
                accessToken = signInResponse.token.accessToken,
                refreshToken = signInResponse.token.refreshToken,
            )
        )
    }
}