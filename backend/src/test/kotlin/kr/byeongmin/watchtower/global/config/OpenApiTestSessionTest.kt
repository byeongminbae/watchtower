package kr.byeongmin.watchtower.global.config

import kr.byeongmin.watchtower.domain.auth.controller.AuthController
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.exception.GlobalExceptionHandler
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import kr.byeongmin.watchtower.global.security.SecurityConfig
import kr.byeongmin.watchtower.global.security.SessionMemberLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertTrue

@SpringJUnitConfig(OpenApiTestSessionTest.TestConfig::class)
@WebAppConfiguration
@ActiveProfiles("local")
class OpenApiTestSessionTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var sessionMemberLoader: SessionMemberLoader

    private val objectMapper = jacksonObjectMapper()
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        reset(sessionMemberLoader)
        mvc = webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `로컬에서 테스트 세션을 발급하면 해당 회원의 실제 권한으로 로그인됨`() {
        /** given */
        val principal = MemberPrinciple(21L, MemberRole.ADMIN.name)
        whenever(sessionMemberLoader.getMemberPrinciple(principal.memberId)).thenReturn(principal)
        val session = MockHttpSession()
        val csrf = getCsrfHeader(session)

        /** when */
        val issueResult = mvc.perform(
            post("/api/v1/auth/test-session")
                .session(session)
                .param("memberId", principal.memberId.toString())
                .header(csrf.first, csrf.second)
        )
        val sessionResult = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        issueResult.andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))
        sessionResult
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))
            .andExpect(jsonPath("$.data.role").value(MemberRole.ADMIN.name))
    }

    @Test
    fun `CSRF 토큰이 없으면 테스트 세션을 발급할 수 없음`() {
        /** given */
        val request = post("/api/v1/auth/test-session").param("memberId", "21")

        /** when */
        val result = mvc.perform(request)

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.ACCESS_DENIED.statusCode))

        verifyNoInteractions(sessionMemberLoader)
    }

    @Test
    fun `정지된 회원은 테스트 세션도 발급할 수 없음`() {
        /** given */
        val memberId = 21L
        whenever(sessionMemberLoader.getMemberPrinciple(memberId))
            .thenThrow(BusinessException(AuthError.BANNED_ACCOUNT))
        val session = MockHttpSession()
        val csrf = getCsrfHeader(session)

        /** when */
        val issueResult = mvc.perform(
            post("/api/v1/auth/test-session")
                .session(session)
                .param("memberId", memberId.toString())
                .header(csrf.first, csrf.second)
        )
        val sessionResult = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        issueResult.andExpect(jsonPath("$.statusCode").value(AuthError.BANNED_ACCOUNT.statusCode))
        sessionResult
            .andExpect(jsonPath("$.statusCode").value(AuthError.UNAUTHORIZED.statusCode))
    }

    @ParameterizedTest
    @ValueSource(strings = ["prod", "dev"])
    fun `운영 및 개발 프로필에서는 테스트 세션 발급 기능을 등록하지 않음`(profile: String) {
        AnnotationConfigApplicationContext().use { applicationContext ->
            /** given */
            applicationContext.environment.setActiveProfiles(profile)
            applicationContext.register(OpenApiConfig::class.java)

            /** when */
            applicationContext.refresh()

            /** then */
            assertTrue(applicationContext.getBeansOfType(OpenApiConfig.TestSessionController::class.java).isEmpty())
        }
    }

    private fun getCsrfHeader(session: MockHttpSession): Pair<String, String> {
        val result = mvc.perform(get("/api/v1/auth/csrf").session(session))
            .andExpect(status().isOk)
            .andReturn()
        val data = objectMapper.readTree(result.response.contentAsString)["data"]
        return data["headerName"].stringValue() to data["token"].stringValue()
    }

    @Configuration
    @EnableWebMvc
    @Import(
        SecurityConfig::class,
        OpenApiConfig::class,
        AuthController::class,
        GlobalExceptionHandler::class
    )
    class TestConfig {
        private val objectMapper = jacksonObjectMapper()

        @Bean
        fun sessionMemberLoader(): SessionMemberLoader {
            return mock()
        }

        @Bean
        fun authService(): AuthService {
            return mock()
        }

        @Bean
        fun objectMapper(): ObjectMapper {
            return objectMapper
        }
    }
}
