package kr.byeongmin.watchtower.domain.auth.service

import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.dto.NaverProfileResponseExternalDto
import kr.byeongmin.watchtower.domain.auth.dto.NaverTokenResponseExternalDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.auth.repository.NaverOAuthRepository
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.domain.member.service.MemberTokenIssuer
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.security.JwtProvider
import kr.byeongmin.watchtower.support.MemberSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTest {
    private val clientId = "네이버에서-부여해준-클라이언트-아이디"
    private val clientSecret = "네이버에서-부여해준-클라이언트-시크릿"
    private val callbackUrl = "네이버에서-우리쪽으로-code와-state를-넘겨주는-콜백주소"
    private val tokenResponse = NaverTokenResponseExternalDto(
        accessToken = "네이버에서-발급한-엑세스-토큰",
        refreshToken = "네이버에서-발급한-리프레시-토큰",
        tokenType = "bearer",
        expiresIn = 3600L,
    )
    private val profileResponse = NaverProfileResponseExternalDto(
        resultCode = "00",
        message = "성공",
        response = NaverProfileResponseExternalDto.NaverProfileDetailDto(
            providerId = "네이버-고객-고유-아이디",
            email = "네이버-고객-이메일",
            nickname = "네이버-고객-닉네임",
            profileImage = "https://naver.com/user/123/profile.png",
        ),
    )

    private val memberRepository = mock<MemberRepository>()
    private val naverOAuthRepository = mock<NaverOAuthRepository>()
    private val memberTokenIssuer = mock<MemberTokenIssuer>()
    private val jwtProvider = mock<JwtProvider>()
    private val memberSupport = MemberSupport()
    private val objectMapper = jacksonObjectMapper()

    private fun createAuthService(restClient: RestClient = mock()): AuthService {
        return AuthService(
            restClient = restClient,
            memberRepository = memberRepository,
            naverOAuthRepository = naverOAuthRepository,
            memberTokenIssuer = memberTokenIssuer,
            jwtProvider = jwtProvider,
            naverClientId = clientId,
            naverClientSecret = clientSecret,
            callbackUrl = callbackUrl,
        )
    }

    @Test
    fun `로그인 상태값이 주어진 상황에서 네이버 로그인 URL을 생성하면 OAuth 설정값을 포함한다`() {
        // Given
        val state = "프론트가-설정하는-CSRF-방지용-랜덤값"
        val authService = createAuthService()

        // When
        val response = authService.getNaverSignInUrl(state)

        // Then
        val url = UriComponentsBuilder.fromUriString(response.data).build()
        assertEquals("https", url.scheme)
        assertEquals("nid.naver.com", url.host)
        assertEquals("/oauth2.0/authorize", url.path)
        assertEquals(
            mapOf(
                "response_type" to listOf("code"),
                "client_id" to listOf(clientId),
                "redirect_uri" to listOf(callbackUrl),
                "state" to listOf(state),
            ),
            url.queryParams,
        )
    }

    @Nested
    inner class NaverCallbackTest {
        private val restClientBuilder = RestClient.builder()
        private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        private val authService = createAuthService(restClientBuilder.build())
        private val member = memberSupport.createMember(profileResponse)
        private val providerId = profileResponse.response.providerId
        private val watchtowerMemberToken = MemberTokenResponseDto(
            accessToken = "와치타워-엑세스-토큰",
            refreshToken = "와치타워-리프레시-토큰",
            accessTokenExpiry = 3600L,
            refreshTokenExpiry = 10000L,
        )
        private val code = "네이버가-콜백으로-넘겨준-인가-코드"
        private val state = "네이버가-콜백으로-넘겨주는-CSRF-방지용-랜덤값"

        @BeforeEach
        fun setUpNaverApiStubs() {
            val tokenUri = UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/token")
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("code", code)
                .queryParam("state", state)
                .build()
                .encode()
                .toUri()

            mockServer.expect(requestTo(tokenUri))
                .andExpect(method(POST))
                .andRespond(
                    withSuccess(
                        objectMapper.writeValueAsString(tokenResponse),
                        APPLICATION_JSON
                    )
                )
            mockServer.expect(requestTo("https://openapi.naver.com/v1/nid/me"))
                .andExpect(method(GET))
                .andExpect(
                    header(
                        AUTHORIZATION,
                        "Bearer ${tokenResponse.accessToken}"
                    )
                )
                .andRespond(
                    withSuccess(
                        objectMapper.writeValueAsString(profileResponse),
                        APPLICATION_JSON
                    )
                )
        }

        @Test
        fun `가입된 네이버 회원인 상황에서 콜백 로그인을 요청하면 기존 회원 토큰을 발급한다`() {
            // Given
            val naverOAuth = NaverOAuth.from(
                member = member,
                naverMemberProfile = profileResponse,
                naverMemberToken = tokenResponse,
            )
            whenever(naverOAuthRepository.existsByProviderId(providerId)).thenReturn(true)
            whenever(naverOAuthRepository.findByProviderId(providerId)).thenReturn(naverOAuth)
            whenever(memberTokenIssuer.signIn(member)).thenReturn(watchtowerMemberToken)

            // When
            val response = authService.signInWithNaverCallback(code = code, state = state)

            // Then
            assertEquals(expected = watchtowerMemberToken, actual = response.data)
            verify(memberRepository, never()).save(any<Member>())
            verify(memberTokenIssuer).signIn(member)
            mockServer.verify()
        }

        @Test
        fun `가입되지 않은 네이버 회원인 상황에서 콜백 로그인을 요청하면 회원가입 후 토큰을 발급한다`() {
            // Given
            whenever(naverOAuthRepository.existsByProviderId(providerId)).thenReturn(false)
            whenever(memberRepository.save(any<Member>())).thenReturn(member)
            whenever(memberTokenIssuer.signIn(member)).thenReturn(watchtowerMemberToken)

            // When
            val response = authService.signInWithNaverCallback(code = code, state = state)

            // Then
            val oauthCaptor = argumentCaptor<NaverOAuth>()
            verify(naverOAuthRepository).save(oauthCaptor.capture())
            assertEquals(providerId, oauthCaptor.firstValue.providerId)
            assertEquals(tokenResponse.accessToken, oauthCaptor.firstValue.accessToken)
            assertEquals(tokenResponse.refreshToken, oauthCaptor.firstValue.refreshToken)
            assertEquals(watchtowerMemberToken, response.data)
            verify(memberTokenIssuer).signIn(member)
            mockServer.verify()
        }
    }

    @Test
    fun `유효한 회원의 리프레시 토큰이 주어진 상황에서 토큰 갱신을 요청하면 새 회원 토큰을 발급한다`() {
        // Given
        val authService = createAuthService()
        val memberId = 31L
        val member = memberSupport.createMember(profileResponse, id = memberId)
        val newMemberTokenResponse = MemberTokenResponseDto(
            accessToken = "와치타워-새-엑세스-토큰",
            refreshToken = "와치타워-새-리프레시-토큰",
            accessTokenExpiry = 3600L,
            refreshTokenExpiry = 10000L
        )
        whenever(jwtProvider.getMemberId("와치타워-리프레시-토큰")).thenReturn(memberId)
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))
        whenever(memberTokenIssuer.rotate(member)).thenReturn(newMemberTokenResponse)

        // When
        val response = authService.renewToken("와치타워-리프레시-토큰")

        // Then
        assertEquals(newMemberTokenResponse, response.data)
        verify(memberTokenIssuer).rotate(member)
    }

    @Test
    fun `회원이 존재하지 않는 상황에서 리프레시 토큰으로 갱신하면 리소스 없음 예외를 던진다`() {
        // Given
        val authService = createAuthService()
        val memberId = 31L
        whenever(jwtProvider.getMemberId("탈퇴한-와치타워-회원의-리프레시-토큰")).thenReturn(memberId)
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.empty())

        // When
        val exception = assertFailsWith<BusinessException> {
            authService.renewToken("탈퇴한-와치타워-회원의-리프레시-토큰")
        }

        // Then
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verifyNoInteractions(memberTokenIssuer)
    }

    @Test
    fun `리프레시 토큰을 가진 회원이 존재하는 상황에서 로그아웃하면 리프레시 토큰을 삭제한다`() {
        // Given
        val authService = createAuthService()
        val memberId = 31L
        val member = memberSupport.createMember(profileResponse, memberId)
        member.rotateRefreshToken("와치타워-리프레시-토큰")
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.of(member))

        // When
        val response = authService.logout(memberId)

        // Then
        assertTrue(response.success)
        assertNull(member.refreshToken)
    }

    @Test
    fun `회원이 존재하지 않는 상황에서 로그아웃하면 리소스 없음 예외를 던진다`() {
        // Given
        val authService = createAuthService()
        val memberId = 31L
        whenever(memberRepository.findById(memberId)).thenReturn(Optional.empty())

        // When
        val exception = assertFailsWith<BusinessException> {
            authService.logout(memberId)
        }

        // Then
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
