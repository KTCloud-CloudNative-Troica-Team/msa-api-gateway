package dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.request.SignInRequest
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.request.SignUpRequest
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.response.CheckValidityResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.response.SignInResponse

interface UserAuthApiGatewayRestController {
    suspend fun signUp(request: SignUpRequest)
    suspend fun signIn(request: SignInRequest): SignInResponse
    suspend fun checkValidity(accessToken: String): CheckValidityResponse
}