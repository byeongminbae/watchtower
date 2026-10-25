package kr.byeongmin.watchtower.domain.auth.controller

import kr.byeongmin.watchtower.domain.auth.dto.MemberSessionResponseDto
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.DefaultCsrfToken
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.security.web.method.annotation.CsrfTokenArgumentResolver
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerTest {
    private val authService = mock<AuthService>()
    private val mvc = MockMvcBuilders.standaloneSetup(AuthController(authService))
        .setCustomArgumentResolvers(
            AuthenticationPrincipalArgumentResolver(),
            CsrfTokenArgumentResolver()
        )
        .build()
    private val principal = MemberPrinciple(21L, MemberRole.NORMAL.name)

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `로그인 state를 전달하면 네이버 인증 URL을 반환함`() {
        /** given */
        val state = "frontend-csrf-state"
        val naverLoginUrl = "https://nid.naver.com/oauth2.0/authorize?state=$state"
        whenever(authService.getNaverSignInUrl(eq(state), any(), any()))
            .thenReturn(SuccessDataResponse(naverLoginUrl))

        /** when */
        val result = mvc.perform(get("/api/v1/auth/naver/url").param("state", state))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(naverLoginUrl))

        verify(authService).getNaverSignInUrl(eq(state), any(), any())
    }

    @Test
    fun `네이버 콜백의 코드와 state를 전달하면 로그인한 회원 정보를 반환함`() {
        /** given */
        val code = "naver-authorization-code"
        val state = "frontend-csrf-state"
        whenever(authService.signInWithNaverCallback(eq(code), eq(state), any(), any()))
            .thenReturn(SuccessDataResponse(MemberSessionResponseDto(principal.memberId, principal.role)))

        /** when */
        val result = mvc.perform(
            get("/api/v1/auth/naver/callback")
                .param("code", code)
                .param("state", state)
        )

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))
            .andExpect(jsonPath("$.data.role").value(principal.role))

        verify(authService).signInWithNaverCallback(eq(code), eq(state), any(), any())
    }

    @Test
    fun `세션을 조회하면 현재 인증된 회원 정보를 반환함`() {
        /** given */
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            emptyList()
        )

        /** when */
        val result = mvc.perform(get("/api/v1/auth/session"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))
            .andExpect(jsonPath("$.data.role").value(principal.role))
    }

    @Test
    fun `CSRF 토큰을 조회하면 요청에 사용할 토큰과 헤더 이름을 반환함`() {
        /** given */
        val csrfToken = DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-token")

        /** when */
        val result = mvc.perform(get("/api/v1/auth/csrf").requestAttr(CsrfToken::class.java.name, csrfToken))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.token").value(csrfToken.token))
            .andExpect(jsonPath("$.data.headerName").value(csrfToken.headerName))
    }
}
