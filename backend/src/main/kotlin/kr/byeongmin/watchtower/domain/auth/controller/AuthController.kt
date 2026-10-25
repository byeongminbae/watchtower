package kr.byeongmin.watchtower.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.security.MemberId
import kr.byeongmin.watchtower.global.security.MemberOnly
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    val authService: AuthService
) {
    @Operation(summary = "네이버 OAuth URL 생성 및 반환")
    @GetMapping("/naver/url")
    fun getNaverSignInUrl(
        @Parameter(description = "CSRF 방지용으로 프론트에서 임의 생성하고 쿠키에 저장시킨 nonce 값")
        @RequestParam(value = "state") state: String
    ): SuccessDataResponse<String> {
        return authService.getNaverSignInUrl(state)
    }

    @Operation(summary = "네이버 로그인 성공 후 콜백. 기존 회원은 로그인, 신규 회원은 가입으로 처리함. 이후 Watchtower JWT 발급")
    @GetMapping("/naver/callback")
    fun signInWithNaverCallback(
        @Parameter(description = "네이버 로그인 인증에 성공하면 반환받는 인증 코드, 네이버 토큰 발급에 사용")
        @RequestParam(value = "code") code: String,
        @Parameter(description = "프론트가 이전 단계에서 생성했던 nonce 값")
        @RequestParam(value = "state") state: String
    ): SuccessDataResponse<MemberTokenResponseDto> {
        return authService.signInWithNaverCallback(code, state)
    }

    @Operation(summary = "Watchtower Token 재발급")
    @PostMapping("/renew")
    fun renewToken(
        @RequestParam(value = "refreshToken") refreshToken: String
    ): SuccessDataResponse<MemberTokenResponseDto> {
        return authService.renewToken(refreshToken)
    }

    @Operation(summary = "Watchtower Refresh Token 삭제")
    @DeleteMapping("/logout")
    @MemberOnly
    fun logout(
        @MemberId memberId: Long
    ): SuccessResponse {
        return authService.logout(memberId)
    }
}
