package kr.byeongmin.watchtower.domain.auth.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.global.response.SuccessResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    @Operation(summary = "네이버 OAuth URL 생성 및 반환")
    @GetMapping("/naver/url")
    fun getNaverLoginUrl(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "네이버 OAuth 콜백 처리 및 Watchtower JWT 발급")
    @GetMapping("/naver/callback")
    fun loginWithNaverCallback(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "네이버 OAuth Token Revocation")
    @DeleteMapping("/naver/revoke")
    fun revokeNaverToken(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "Watchtower Token 재발급")
    @PostMapping("/renew")
    fun renewToken(): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "Watchtower Refresh Token 삭제")
    @DeleteMapping("/logout")
    fun logout(): SuccessResponse<String> {
        return SuccessResponse("")
    }
}