package kr.byeongmin.watchtower.global.security

import kr.byeongmin.watchtower.domain.auth.accessor.DiscordAccountAccessor
import kr.byeongmin.watchtower.domain.auth.controller.AuthController
import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.domain.auth.service.DiscordAuthService
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.accessor.PlanAccessor
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.external.discord.dto.DiscordUserResponseDto
import kr.byeongmin.watchtower.global.alert.AdminNotifier
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.exception.GlobalExceptionHandler
import kr.byeongmin.watchtower.global.utils.ifNullThrow
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
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.util.UriUtils
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val FRONTEND_URL = "https://watchtower.test"
private const val LOGIN_CALLBACK_URL = "$FRONTEND_URL/login/callback"
private const val AUTHORIZATION_PATH = "/api/v1/oauth2/authorization/discord"
private const val CALLBACK_PATH = "/api/v1/oauth2/callback/discord"
private const val AUTHORIZATION_REQUEST_NOT_FOUND = "authorization_request_not_found"

@SpringJUnitConfig(SessionAuthenticationTest.TestConfig::class)
@WebAppConfiguration
@TestPropertySource(properties = ["watchtower.frontend-url=$FRONTEND_URL"])
class SessionAuthenticationTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var discordClient: DiscordClient

    @Autowired
    private lateinit var discordAccountAccessor: DiscordAccountAccessor

    @Autowired
    private lateinit var timeProvider: TimeProvider

    @Autowired
    private lateinit var sessionMemberLoader: SessionMemberLoader

    @Autowired
    private lateinit var accessTokenResponseClient: OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>

    private val objectMapper = jacksonObjectMapper()
    private lateinit var mvc: MockMvc
    private val principal = MemberPrinciple(21L, MemberRole.NORMAL.name)
    private val member = mock<Member> {
        on { id } doReturn principal.memberId
        on { role } doReturn MemberRole.NORMAL
    }
    private val discordUser = DiscordUserResponseDto("provider-id", "member", "회원", null)
    private val discordAccount = DiscordAccount.of(member, discordUser.id, "direct-message-channel-id")

    @BeforeEach
    fun setUp() {
        reset(discordClient, discordAccountAccessor, timeProvider, sessionMemberLoader, accessTokenResponseClient)
        whenever(accessTokenResponseClient.getTokenResponse(any())).thenReturn(
            OAuth2AccessTokenResponse.withToken("access")
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(604800)
                .build()
        )
        whenever(discordClient.fetchUser("access")).thenReturn(discordUser)
        whenever(discordAccountAccessor.findDiscordAccount(discordUser.id))
            .thenReturn(discordAccount)
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
    fun `Discord 로그인을 시작하면 state 와 PKCE 검증값을 담아 Discord 인증 화면으로 보냄`() {
        /** when */
        val result = mvc.perform(get(AUTHORIZATION_PATH).session(MockHttpSession()))

        /** then */
        result.andExpect(status().isFound)
        val authorizationUrl = result.andReturn().response.redirectedUrl.ifNullThrow()
        assertTrue(authorizationUrl.startsWith("https://discord.com/oauth2/authorize?"))
        assertEquals("discord-application-id", getQueryParameter(authorizationUrl, "client_id"))
        assertEquals("$FRONTEND_URL/bff/oauth2/callback/discord", getQueryParameter(authorizationUrl, "redirect_uri"))
        assertEquals("identify guilds.join", getQueryParameter(authorizationUrl, "scope"))
        assertEquals("S256", getQueryParameter(authorizationUrl, "code_challenge_method"))
        assertNotNull(getQueryParameter(authorizationUrl, "code_challenge"))
        assertNotNull(getQueryParameter(authorizationUrl, "state"))
    }

    @Test
    fun `Discord 인증을 마치면 세션 ID를 바꾸고 프론트 로그인 콜백 화면으로 보내며 이후 요청에서도 인증을 유지함`() {
        /** given */
        val session = MockHttpSession()
        val state = startDiscordLogin(session)
        val originalId = session.id

        /** when */
        val callbackResult = finishDiscordLogin(session, state)
        val sessionResult = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        callbackResult.andExpect(redirectedUrl(LOGIN_CALLBACK_URL))
        assertNotEquals(originalId, session.id)
        sessionResult
            .andExpect(jsonPath("$.data.memberId").value(principal.memberId))
        verify(sessionMemberLoader).getMemberPrinciple(principal.memberId)
    }

    @Test
    fun `Discord 계정이 연결된 회원이 로그인한 채로 다시 Discord 인증을 마치면 오류와 함께 돌려보내고 기존 로그인은 유지함`() {
        /** given */
        val session = signIn()
        whenever(discordAccountAccessor.findDiscordAccountOfMember(principal.memberId)).thenReturn(discordAccount)
        val state = startDiscordLogin(session)

        /** when */
        val callbackResult = finishDiscordLogin(session, state)
        val sessionResult = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        callbackResult.andExpect(
            redirectedUrl("$LOGIN_CALLBACK_URL?error=${SocialAccountError.PROVIDER_ALREADY_LINKED.statusCode}")
        )
        sessionResult.andExpect(jsonPath("$.data.memberId").value(principal.memberId))
        verify(discordAccountAccessor, never()).save(any())
    }

    @Test
    fun `로그인을 막는 오류가 나면 오류 코드와 함께 프론트 로그인 콜백 화면으로 보내고 로그인하지 않음`() {
        /** given */
        doThrow(BusinessException(AuthError.BANNED_ACCOUNT)).whenever(member).ensureSignInAllowed()
        val session = MockHttpSession()
        val state = startDiscordLogin(session)

        /** when */
        val callbackResult = finishDiscordLogin(session, state)
        val sessionResult = mvc.perform(get("/api/v1/auth/session").session(session))

        /** then */
        callbackResult.andExpect(redirectedUrl("$LOGIN_CALLBACK_URL?error=${AuthError.BANNED_ACCOUNT.statusCode}"))
        sessionResult.andExpect(jsonPath("$.statusCode").value(AuthError.UNAUTHORIZED.statusCode))
    }

    @Test
    fun `로그인을 시작하지 않은 세션으로 Discord 콜백이 오면 토큰을 발급받지 않고 오류와 함께 돌려보냄`() {
        /** when */
        val result = finishDiscordLogin(MockHttpSession(), "state")

        /** then */
        result.andExpect(redirectedUrl("$LOGIN_CALLBACK_URL?error=$AUTHORIZATION_REQUEST_NOT_FOUND"))
        verifyNoInteractions(accessTokenResponseClient, discordClient)
    }

    @Test
    fun `저장한 state와 콜백 state가 다르면 토큰을 발급받지 않고 오류와 함께 돌려보냄`() {
        /** given */
        val session = MockHttpSession()
        startDiscordLogin(session)

        /** when */
        val result = finishDiscordLogin(session, "other-state")

        /** then */
        result.andExpect(redirectedUrl("$LOGIN_CALLBACK_URL?error=$AUTHORIZATION_REQUEST_NOT_FOUND"))
        verifyNoInteractions(accessTokenResponseClient, discordClient)
    }

    @Test
    fun `이미 사용한 state로는 다시 로그인하지 않음`() {
        /** given */
        val session = MockHttpSession()
        val state = startDiscordLogin(session)
        finishDiscordLogin(session, state).andExpect(redirectedUrl(LOGIN_CALLBACK_URL))
        clearInvocations(accessTokenResponseClient)

        /** when */
        val result = finishDiscordLogin(session, state)

        /** then */
        result.andExpect(redirectedUrl("$LOGIN_CALLBACK_URL?error=$AUTHORIZATION_REQUEST_NOT_FOUND"))
        verifyNoInteractions(accessTokenResponseClient)
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
        val session = MockHttpSession()
        val csrf = getCsrfHeader(session)
        val state = startDiscordLogin(session)
        finishDiscordLogin(session, state).andExpect(redirectedUrl(LOGIN_CALLBACK_URL))

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
    fun `로그인 도중인 익명 세션도 유효한 CSRF 토큰으로 로그아웃할 수 있음`() {
        /** given */
        val session = MockHttpSession()
        startDiscordLogin(session)
        val csrf = getCsrfHeader(session)

        /** when */
        val result = mvc.perform(delete("/api/v1/auth/logout").session(session).header(csrf.first, csrf.second))

        /** then */
        result
            .andExpect(jsonPath("$.isSuccess").value(true))

        assertTrue(session.isInvalid)
        verifyNoInteractions(accessTokenResponseClient)
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


    private fun startDiscordLogin(session: MockHttpSession): String {
        val result = mvc.perform(get(AUTHORIZATION_PATH).session(session))
            .andExpect(status().isFound)
            .andReturn()
        return getQueryParameter(result.response.redirectedUrl.ifNullThrow(), "state").ifNullThrow()
    }

    private fun finishDiscordLogin(session: MockHttpSession, state: String): ResultActions {
        return mvc.perform(
            get(CALLBACK_PATH)
                .session(session)
                .param("code", "code")
                .param("state", state)
        )
    }

    private fun signIn(): MockHttpSession {
        val session = MockHttpSession()
        val state = startDiscordLogin(session)
        finishDiscordLogin(session, state).andExpect(redirectedUrl(LOGIN_CALLBACK_URL))
        return session
    }

    private fun getCsrfHeader(session: MockHttpSession): Pair<String, String> {
        val result = mvc.perform(get("/api/v1/auth/csrf").session(session))
            .andExpect(status().isOk)
            .andReturn()
        val data = objectMapper.readTree(result.response.contentAsString)["data"]
        return data["headerName"].stringValue() to data["token"].stringValue()
    }

    private fun getQueryParameter(url: String, name: String): String? {
        val value = UriComponentsBuilder.fromUriString(url).build().queryParams.getFirst(name) ?: return null
        return UriUtils.decode(value, StandardCharsets.UTF_8)
    }

    @Configuration
    @EnableWebMvc
    @Import(
        SecurityConfig::class,
        AuthController::class,
        DiscordAuthService::class,
        GlobalExceptionHandler::class,
        ProtectedController::class
    )
    class TestConfig {
        private val objectMapper = jacksonObjectMapper()

        @Bean
        fun adminNotifier(): AdminNotifier {
            return mock()
        }

        @Bean
        fun discordClient(): DiscordClient {
            return mock()
        }

        @Bean
        fun memberAccessor(): MemberAccessor {
            return mock()
        }

        @Bean
        fun discordAccountAccessor(): DiscordAccountAccessor {
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
        fun sessionMemberLoader(): SessionMemberLoader {
            return mock()
        }

        @Bean
        fun objectMapper(): ObjectMapper {
            return objectMapper
        }

        @Bean
        fun clientRegistrationRepository(): ClientRegistrationRepository {
            return InMemoryClientRegistrationRepository(
                ClientRegistration.withRegistrationId("discord")
                    .clientId("discord-application-id")
                    .clientSecret("discord-application-client-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("$FRONTEND_URL/bff/oauth2/callback/{registrationId}")
                    .scope("identify", "guilds.join")
                    .authorizationUri("https://discord.com/oauth2/authorize")
                    .tokenUri("https://discord.com/api/v10/oauth2/token")
                    .build()
            )
        }

        @Bean
        fun accessTokenResponseClient(): OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
            return mock()
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
