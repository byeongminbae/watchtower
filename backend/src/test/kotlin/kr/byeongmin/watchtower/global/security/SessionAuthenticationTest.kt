package kr.byeongmin.watchtower.global.security

import kr.byeongmin.watchtower.domain.auth.controller.AuthController
import kr.byeongmin.watchtower.domain.auth.service.AuthService
import kr.byeongmin.watchtower.domain.auth.accessor.NaverOAuthAccessor
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.accessor.PlanAccessor
import kr.byeongmin.watchtower.external.naver.client.NaverAuthClient
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.exception.GlobalExceptionHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringJUnitConfig(SessionAuthenticationTest.TestConfig::class)
@WebAppConfiguration
class SessionAuthenticationTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var naverAuthClient: NaverAuthClient

    @Autowired
    private lateinit var naverOAuthAccessor: NaverOAuthAccessor

    @Autowired
    private lateinit var timeProvider: TimeProvider

    @Autowired
    private lateinit var sessionMemberLoader: SessionMemberLoader

    private val objectMapper = jacksonObjectMapper()
    private lateinit var mvc: MockMvc
    private val principal = MemberPrinciple(21L, MemberRole.NORMAL.name)

    @BeforeEach
    fun setUp() {
        reset(naverAuthClient, naverOAuthAccessor, timeProvider, sessionMemberLoader)
        whenever(naverAuthClient.getAuthorizationRequest("state")).thenReturn(
            OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .clientId("client")
                .redirectUri("https://watchtower.test/login/callback")
                .state("state")
                .build()
        )
        val token = NaverTokenResponseDto("access", "refresh", "bearer", 3600L)
        val profile = NaverProfileResponseDto(
            "00",
            "성공",
            NaverProfileResponseDto.NaverProfileDetailDto(
                "provider-id",
                "member@example.com",
                "회원",
                "https://example.com/profile.png"
            )
        )
        val member = mock<Member> {
            on { id } doReturn principal.memberId
            on { role } doReturn MemberRole.NORMAL
        }
        whenever(naverAuthClient.fetchToken("code", "state")).thenReturn(token)
        whenever(naverAuthClient.fetchProfile(token.accessToken)).thenReturn(profile)
        whenever(naverOAuthAccessor.findByProviderId(profile.response.providerId))
            .thenReturn(NaverOAuth.from(member, profile, token))
        whenever(timeProvider.now()).thenReturn(LocalDateTime.of(2026, 10, 26, 12, 0))
        whenever(sessionMemberLoader.getMemberPrinciple(principal.memberId)).thenReturn(principal)
        mvc = webAppContextSetup(context)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    @AfterEach
    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `로그인 콜백은 세션 ID를 바꾸고 이후 요청에서도 인증을 유지함`() {
        /** given */
        val session = createOAuthSession()
        val originalId = session.id

        /** when */
        val callbackResult = mvc.perform(
            get("/api/v1/auth/naver/callback")
                .session(session)
                .param("code", "code")
                .param("state", "state")
        )
        val sessionResult = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        callbackResult.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))
        assertNotEquals(originalId, session.id)
        sessionResult
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))
        verify(sessionMemberLoader).getMemberPrinciple(principal.memberId)
    }

    @Test
    fun `로그인한 세션의 CSRF 토큰으로 변경 요청을 보내면 성공함`() {
        /** given */
        val session = signIn()
        val csrf = getCsrfHeader(session)

        /** when */
        val result = mvc.perform(post("/session-test/write").session(session).header(csrf.first, csrf.second))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(true))
    }

    @Test
    fun `로그인 전에 받은 CSRF 토큰은 로그인 후 변경 요청에 사용할 수 없음`() {
        /** given */
        val session = createOAuthSession()
        val csrf = getCsrfHeader(session)
        mvc.perform(
            get("/api/v1/auth/naver/callback")
                .session(session)
                .param("code", "code")
                .param("state", "state")
        )
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))

        /** when */
        val result = mvc.perform(post("/session-test/write").session(session).header(csrf.first, csrf.second))

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.ACCESS_DENIED.statusCode))
    }

    @Test
    fun `CSRF 토큰을 보내지 않은 변경 요청은 거부함`() {
        /** given */
        val session = signIn()

        /** when */
        val result = mvc.perform(post("/session-test/write").session(session))

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.ACCESS_DENIED.statusCode))
    }

    @Test
    fun `현재 세션과 다른 CSRF 토큰으로 보낸 변경 요청은 거부함`() {
        /** given */
        val session = signIn()
        val csrf = getCsrfHeader(session)

        /** when */
        val result = mvc.perform(post("/session-test/write").session(session).header(csrf.first, "wrong-token"))

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.ACCESS_DENIED.statusCode))
    }

    @Test
    fun `유효한 CSRF 토큰으로 로그아웃하면 세션과 쿠키가 만료됨`() {
        /** given */
        val session = signIn()
        val csrf = getCsrfHeader(session)

        /** when */
        val result = mvc.perform(
            delete("/api/v1/auth/logout").session(session).header(csrf.first, csrf.second)
        )
        val sessionResult = mvc.perform(get("/api/v1/auth/session"))

        /** then */
        result.andExpect(jsonPath("$.isSuccess").value(true))
        assertTrue(session.isInvalid)
        val cookie = result.andReturn().response.cookies.last { it.name == "JSESSIONID" }
        assertEquals(0, cookie.maxAge)
        sessionResult
            .andExpect(jsonPath("$.statusCode").value(AuthError.UNAUTHORIZED.statusCode))
    }

    @Test
    fun `익명 세션도 유효한 CSRF 토큰으로 로그아웃할 수 있음`() {
        /** given */
        val session = createOAuthSession()
        val csrf = getCsrfHeader(session)

        /** when */
        val result = mvc.perform(delete("/api/v1/auth/logout").session(session).header(csrf.first, csrf.second))

        /** then */
        result
            .andExpect(jsonPath("$.isSuccess").value(true))

        assertTrue(session.isInvalid)
        verify(naverAuthClient, never()).fetchToken(any(), any())
    }

    @Test
    fun `CSRF 토큰 없이 로그인 URL을 요청하면 네이버 인증을 진행하지 않음`() {
        /** given */
        val request = get("/api/v1/auth/naver/url").param("state", "attacker-known-state")

        /** when */
        val result = mvc.perform(request)

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.ACCESS_DENIED.statusCode))

        verifyNoInteractions(naverAuthClient)
    }

    @Test
    fun `세션에 저장한 state가 없으면 네이버 로그인을 진행하지 않음`() {
        /** given */
        val request = get("/api/v1/auth/naver/callback")
            .param("code", "code")
            .param("state", "state")

        /** when */
        val result = mvc.perform(request)

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.INVALID_OAUTH_STATE.statusCode))

        verify(naverAuthClient, never()).fetchToken(any(), any())
    }

    @Test
    fun `저장한 state와 콜백 state가 다르면 네이버 로그인을 진행하지 않음`() {
        /** given */
        val session = createOAuthSession()

        /** when */
        val result = mvc.perform(
            get("/api/v1/auth/naver/callback")
                .session(session)
                .param("code", "code")
                .param("state", "other-state")
        )

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.INVALID_OAUTH_STATE.statusCode))

        verify(naverAuthClient, never()).fetchToken(any(), any())
    }

    @Test
    fun `이미 사용한 state로는 네이버 로그인을 다시 진행하지 않음`() {
        /** given */
        val session = signIn()
        clearInvocations(naverAuthClient)

        /** when */
        val result = mvc.perform(
            get("/api/v1/auth/naver/callback")
                .session(session)
                .param("code", "code")
                .param("state", "state")
        )

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.INVALID_OAUTH_STATE.statusCode))

        verify(naverAuthClient, never()).fetchToken(any(), any())
    }

    @Test
    fun `Bearer 토큰만 보낸 요청은 세션 인증으로 인정하지 않음`() {
        /** given */
        val request = get("/api/v1/auth/session").header("Authorization", "Bearer old-token")

        /** when */
        val result = mvc.perform(request)

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(AuthError.UNAUTHORIZED.statusCode))

        verifyNoInteractions(sessionMemberLoader)
    }

    @Test
    fun `회원 권한이 바뀌면 다음 요청부터 변경된 권한을 적용함`() {
        /** given */
        val session = signIn()
        whenever(sessionMemberLoader.getMemberPrinciple(principal.memberId))
            .thenReturn(MemberPrinciple(principal.memberId, MemberRole.ADMIN.name), principal)

        /** when */
        val adminResult = mvc.perform(get("/session-test/admin").session(session))
        val memberResult = mvc.perform(get("/session-test/admin").session(session))

        /** then */
        adminResult
            .andExpect(jsonPath("$.isSuccess").value(true))
        memberResult
            .andExpect(jsonPath("$.statusCode").value(AuthError.ACCESS_DENIED.statusCode))
    }

    @ParameterizedTest
    @EnumSource(
        value = AuthError::class,
        names = ["UNAUTHORIZED", "ALREADY_DELETED_ACCOUNT", "BANNED_ACCOUNT", "ACCOUNT_NOT_FOUND"]
    )
    fun `세션 회원을 더 이상 인증할 수 없으면 해당 오류를 반환하고 세션을 무효화함`(error: AuthError) {
        /** given */
        val session = signIn()
        whenever(sessionMemberLoader.getMemberPrinciple(principal.memberId))
            .thenThrow(BusinessException(error))

        /** when */
        val result = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        result
            .andExpect(jsonPath("$.statusCode").value(error.statusCode))

        assertTrue(session.isInvalid)
    }

    @Test
    fun `회원 조회 중 다른 요청이 세션을 무효화해도 인증 세션을 다시 만들지 않음`() {
        /** given */
        val session = signIn()
        whenever(sessionMemberLoader.getMemberPrinciple(principal.memberId)).thenAnswer {
            session.invalidate()
            principal
        }

        /** when */
        val result = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        result.andExpect(jsonPath("$.data.memberId").value(principal.memberId))
        assertTrue(session.isInvalid)
        assertNull(
            result.andReturn().request.getSession(false)
                ?.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
        )
    }

    private fun createOAuthSession(): MockHttpSession {
        val csrfResult = mvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk)
            .andReturn()
        val session = csrfResult.request.getSession(false) as MockHttpSession
        val csrfData = objectMapper.readTree(csrfResult.response.contentAsString)["data"]
        mvc.perform(
            get("/api/v1/auth/naver/url")
                .session(session)
                .header(csrfData["headerName"].stringValue(), csrfData["token"].stringValue())
                .param("state", "state")
        )
            .andExpect(jsonPath("$.isSuccess").value(true))
        return session
    }

    private fun signIn(): MockHttpSession {
        val session = createOAuthSession()
        mvc.perform(
            get("/api/v1/auth/naver/callback")
                .session(session)
                .param("code", "code")
                .param("state", "state")
        )
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))
        return session
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
        AuthController::class,
        AuthService::class,
        GlobalExceptionHandler::class,
        ProtectedController::class
    )
    class TestConfig {
        private val objectMapper = jacksonObjectMapper()

        @Bean
        fun naverAuthClient(): NaverAuthClient {
            return mock()
        }

        @Bean
        fun memberAccessor(): MemberAccessor {
            return mock()
        }

        @Bean
        fun naverOAuthAccessor(): NaverOAuthAccessor {
            return mock()
        }

        @Bean
        fun planAccessor(): PlanAccessor {
            return mock()
        }

        @Bean
        fun timeProvider(): TimeProvider {
            return mock()
        }

        @Bean
        fun transactionManager(): PlatformTransactionManager {
            val transactionStatus = mock<TransactionStatus>()
            return mock {
                on { getTransaction(any()) } doReturn transactionStatus
            }
        }

        @Bean
        fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
            return TransactionTemplate(transactionManager)
        }

        @Bean
        fun sessionMemberLoader(): SessionMemberLoader {
            return mock()
        }

        @Bean
        fun objectMapper(): ObjectMapper {
            return objectMapper
        }
    }

    @RestController
    class ProtectedController {
        @PostMapping("/session-test/write")
        @MemberOnly
        fun write(): SuccessResponse {
            return SuccessResponse()
        }

        @GetMapping("/session-test/admin")
        @AdminOnly
        fun getAdminResource(): SuccessResponse {
            return SuccessResponse()
        }
    }
}
