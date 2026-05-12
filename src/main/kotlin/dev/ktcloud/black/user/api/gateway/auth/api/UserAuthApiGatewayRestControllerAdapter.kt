package dev.ktcloud.black.user.api.gateway.auth.api

import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.UserAuthApiGatewayRestController
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.request.SignInRequest
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.request.SignUpRequest
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.response.CheckValidityResponse
import dev.ktcloud.black.user.api.gateway.adapter.presentation.web.inbound.auth.response.SignInResponse
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.CheckValidityQuery
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.SignInCommand
import dev.ktcloud.black.user.api.gateway.application.auth.port.inbound.SignUpCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class UserAuthApiGatewayRestControllerAdapter(
    private val checkValidityQuery: CheckValidityQuery,
    private val signUpCommand: SignUpCommand,
    private val signInCommand: SignInCommand,
): UserAuthApiGatewayRestController {
    @Operation(summary = "회원가입")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping("/signup")
    override suspend fun signUp(@RequestBody request: SignUpRequest) {
        signUpCommand.signUp(
            SignUpCommand.In(
                email = request.email,
                plainPassword = request.password,
                name = request.name,
            )
        )
    }

    @Operation(summary = "로그인")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @PostMapping("/signin")
    override suspend fun signIn(@RequestBody request: SignInRequest): SignInResponse {
        val result = signInCommand.signIn(
            SignInCommand.In(
                email = request.email,
                plainPassword = request.password,
            )
        )

        return SignInResponse(
            user = result.user,
            token = result.token,
        )
    }

    @Operation(summary = "토큰 유효성 체크")
    @ApiResponse(responseCode = "200", description = "토큰 유효성 체크 성공")
    @ResponseStatus(value = HttpStatus.OK)
    @GetMapping("/check")
    override suspend fun checkValidity(@RequestHeader("Authorization") accessToken: String): CheckValidityResponse {
        val token = accessToken.substringAfter("Bearer").trim()

        val result = checkValidityQuery.checkValidity(
            CheckValidityQuery.In(token)
        )

        return CheckValidityResponse(
            user = result.user,
        )
    }
}