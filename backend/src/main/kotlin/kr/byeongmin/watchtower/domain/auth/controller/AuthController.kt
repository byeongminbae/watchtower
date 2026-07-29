package kr.byeongmin.watchtower.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.global.response.SuccessDataResponse
import kr.byeongmin.watchtower.global.response.SuccessResponse
import kr.byeongmin.watchtower.global.security.AuthenticatedUser
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    val authService: AuthService,
) {
    @Operation(summary = "네이버 OAuth URL 생성 및 반환")
    @GetMapping("/naver/url")
    fun getNaverLoginUrl(
        @Parameter(description = "CSRF 방지용으로 임의 생성된 nonce 값")
        @RequestParam(value = "state") state: String
    ): SuccessDataResponse<String> {
        return authService.getNaverLoginUrl(state)
    }

    @Operation(summary = "네이버 로그인 처리 및 회원가입 처리 후 Watchtower JWT 발급")
    @GetMapping("/naver/callback")
    fun loginWithNaverCallback(
        @RequestParam(value = "code") code: String,
        @RequestParam(value = "state") state: String
    ): SuccessDataResponse<MemberTokenResponseDto> {
        return authService.loginWithNaverCallback(code, state)
    }

    @Operation(summary = "네이버 OAuth Token Revocation")
    @DeleteMapping("/naver/revoke")
    @AuthenticatedUser
    fun revokeNaverToken(): SuccessResponse {
        return SuccessResponse()
    }

    @Operation(summary = "Watchtower Token 재발급")
    @PostMapping("/renew")
    fun renewToken(): SuccessResponse {
        return SuccessResponse()
    }

    @Operation(summary = "Watchtower Refresh Token 삭제")
    @DeleteMapping("/logout")
    @AuthenticatedUser
    fun logout(): SuccessResponse {
        return SuccessResponse()
    }
}
