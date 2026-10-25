package kr.byeongmin.watchtower.domain.auth.controller

import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerTest {
    private val authService = mock<AuthService>()
    private val mvc = MockMvcBuilders.standaloneSetup(AuthController(authService))
        .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
        .build()
    private val watchtowerTokens = MemberTokenResponseDto(
        accessToken = "watchtower-access-token",
        refreshToken = "watchtower-refresh-token",
        accessTokenExpiresIn = 60_000,
        refreshTokenExpiresIn = 600_000
    )

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `프론트가 CSRF 방지용 state를 보내면 네이버 로그인 URL을 받을 수 있음`() {
        /** given */
        val state = "frontend-csrf-state"
        val naverLoginUrl = "https://nid.naver.com/oauth2.0/authorize?state=$state"
        whenever(authService.getNaverSignInUrl(state)).thenReturn(SuccessDataResponse(naverLoginUrl))

        /** when */
        val result = mvc
            .perform(get("/api/v1/auth/naver/url").param("state", state))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(naverLoginUrl))
    }

    @Test
    fun `네이버 로그인 성공 후 네이버가 코드와 state를 콜백으로 넘겨주면 Watchtower 로그인 토큰을 반환함`() {
        /** given */
        val code = "naver-authorization-code"
        val state = "frontend-csrf-state"
        whenever(authService.signInWithNaverCallback(code, state))
            .thenReturn(SuccessDataResponse(watchtowerTokens))

        /** when */
        val result = mvc
            .perform(
                get("/api/v1/auth/naver/callback")
                    .param("code", code)
                    .param("state", state)
            )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value(watchtowerTokens.accessToken))
            .andExpect(jsonPath("$.data.refreshToken").value(watchtowerTokens.refreshToken))
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(watchtowerTokens.accessTokenExpiresIn))
            .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(watchtowerTokens.refreshTokenExpiresIn))
    }

    @Test
    fun `리프레시 토큰을 보내면 새 Watchtower 토큰을 받을 수 있음`() {
        /** given */
        val refreshToken = "existing-watchtower-refresh-token"
        whenever(authService.renewToken(refreshToken))
            .thenReturn(SuccessDataResponse(watchtowerTokens))

        /** when */
        val result = mvc
            .perform(post("/api/v1/auth/renew").param("refreshToken", refreshToken))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value(watchtowerTokens.accessToken))
            .andExpect(jsonPath("$.data.refreshToken").value(watchtowerTokens.refreshToken))
            .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(watchtowerTokens.accessTokenExpiresIn))
            .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(watchtowerTokens.refreshTokenExpiresIn))
    }

    @Test
    fun `로그아웃을 요청하면 인증 정보의 회원 ID로 로그아웃함`() {
        /** given */
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            MemberPrinciple(21L, "NORMAL"),
            null,
            emptyList()
        )
        whenever(authService.logout(21L)).thenReturn(SuccessResponse())

        /** when */
        val result = mvc
            .perform(delete("/api/v1/auth/logout"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))
        verify(authService).logout(21L)
    }
}
