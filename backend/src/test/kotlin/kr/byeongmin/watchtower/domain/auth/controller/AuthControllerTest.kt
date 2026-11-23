package kr.byeongmin.watchtower.domain.auth.controller

import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
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
    private val mvc = MockMvcBuilders.standaloneSetup(AuthController())
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
