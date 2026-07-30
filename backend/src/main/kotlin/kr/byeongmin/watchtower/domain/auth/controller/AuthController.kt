package kr.byeongmin.watchtower.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.global.response.SuccessDataResponse
import kr.byeongmin.watchtower.global.response.SuccessResponse
import kr.byeongmin.watchtower.global.security.AuthenticatedUser
import kr.byeongmin.watchtower.global.security.MemberId
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    val authService: AuthService,
) {
    @Operation(summary = "네이버 OAuth URL 생성 및 반환")
    @GetMapping("/naver/url")
    fun getNaverSignInUrl(
        @Parameter(description = "CSRF 방지용으로 임의 생성된 nonce 값")
        @RequestParam(value = "state") state: String
    ): SuccessDataResponse<String> {
        return authService.getNaverSignInUrl(state)
    }

    @Operation(summary = "네이버 로그인 처리 및 회원가입 처리 후 Watchtower JWT 발급")
    @GetMapping("/naver/callback")
    fun signInWithNaverCallback(
        @RequestParam(value = "code") code: String,
        @RequestParam(value = "state") state: String
    ): SuccessDataResponse<MemberTokenResponseDto> {
        return authService.signInWithNaverCallback(code, state)
    }

    @Operation(summary = "Watchtower Token 재발급")
    @PostMapping("/renew")
    @AuthenticatedUser
    fun renewToken(
        @RequestParam(value = "refreshToken") refreshToken: String,
    ): SuccessDataResponse<MemberTokenResponseDto> {
        return authService.renewToken(refreshToken)
    }

    @Operation(summary = "Watchtower Refresh Token 삭제")
    @DeleteMapping("/logout")
    @AuthenticatedUser
    fun logout(
        @MemberId memberId: Long,
    ): SuccessResponse {
        return authService.logout(memberId)
    }
}
