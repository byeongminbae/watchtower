package kr.byeongmin.watchtower.domain.auth.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.domain.auth.accessor.NaverOAuthAccessor
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.accessor.PlanAccessor
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.external.naver.client.NaverAuthClient
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class AuthServiceTest {
    private val memberAccessor = mock<MemberAccessor>()
    private val naverOAuthAccessor = mock<NaverOAuthAccessor>()
    private val planAccessor = mock<PlanAccessor>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }
    private val naverAuthClient = mock<NaverAuthClient>()
    private val authorizationRequestRepository = mock<AuthorizationRequestRepository<OAuth2AuthorizationRequest>>()
    private val sessionAuthenticationStrategy = mock<SessionAuthenticationStrategy>()
    private val securityContextRepository = mock<SecurityContextRepository>()
    private val transactionStatus = mock<TransactionStatus>()
    private val transactionManager = mock<PlatformTransactionManager> {
        on { getTransaction(any()) } doReturn transactionStatus
    }
    private val request = mock<HttpServletRequest>()
    private val response = mock<HttpServletResponse>()
    private val freePlan = mock<Plan> {
        on { type } doReturn PlanType.FREE
    }
    private val authService = AuthService(
        naverAuthClient = naverAuthClient,
        memberAccessor = memberAccessor,
        naverOAuthAccessor = naverOAuthAccessor,
        planAccessor = planAccessor,
        timeProvider = timeProvider,
        authorizationRequestRepository = authorizationRequestRepository,
        sessionAuthenticationStrategy = sessionAuthenticationStrategy,
        securityContextRepository = securityContextRepository,
        transactionTemplate = TransactionTemplate(transactionManager)
    )

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `네이버 로그인 URL을 조회하면 로그인 요청을 세션에 저장하고 인증 URL을 반환함`() {
        /** given */
        val state = "state"
        val authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
            .clientId("naver-client-id")
            .state(state)
            .build()
        whenever(naverAuthClient.getAuthorizationRequest(state)).thenReturn(authorizationRequest)

        /** when */
        val result = authService.getNaverSignInUrl(state, request, response)

        /** then */
        assertEquals(authorizationRequest.authorizationRequestUri, result.data)
        verify(authorizationRequestRepository).saveAuthorizationRequest(authorizationRequest, request, response)
    }

    @Nested
    inner class NaverCallbackTest {
        private val tokenResponse = NaverTokenResponseDto(
            accessToken = "naver-access",
            refreshToken = "naver-refresh",
            tokenType = "bearer",
            expiresIn = 3600L
        )
        private val profileResponse = NaverProfileResponseDto(
            resultCode = "00",
            message = "성공",
            response = NaverProfileResponseDto.NaverProfileDetailDto(
                providerId = "네이버-고객-고유-아이디",
                email = "네이버-고객-이메일",
                nickname = "네이버-고객-닉네임",
                profileImage = "https://naver.com/user/123/profile.png"
            )
        )
        private val member = mock<Member> {
            on { id } doReturn 31L
            on { role } doReturn MemberRole.NORMAL
        }
        private val providerId = profileResponse.response.providerId
        private val code = "code"
        private val state = "state"

        @BeforeEach
        fun setUpNaverApiStubs() {
            whenever(authorizationRequestRepository.removeAuthorizationRequest(request, response)).thenReturn(mock())
            whenever(naverAuthClient.fetchToken(code, state)).thenReturn(tokenResponse)
            whenever(naverAuthClient.fetchProfile(tokenResponse.accessToken)).thenReturn(profileResponse)
        }

        @Test
        fun `이미 가입한 네이버 회원은 로그인 시각을 저장한 뒤 세션 인증을 저장함`() {
            /** given */
            val naverOAuth = NaverOAuth.from(member, profileResponse, tokenResponse)
            whenever(naverOAuthAccessor.findByProviderId(providerId)).thenReturn(naverOAuth)

            /** when */
            val result = authService.signInWithNaverCallback(code, state, request, response)

            /** then */
            assertEquals(31L, result.data.memberId)
            assertEquals(MemberRole.NORMAL.name, result.data.role)
            val authentication = argumentCaptor<Authentication>()
            val context = argumentCaptor<SecurityContext>()
            inOrder(
                authorizationRequestRepository,
                member,
                transactionManager,
                sessionAuthenticationStrategy,
                securityContextRepository
            ) {
                verify(authorizationRequestRepository).removeAuthorizationRequest(request, response)
                verify(member).ensureSignInAllowed()
                verify(member).completeSignIn(now)
                verify(transactionManager).commit(transactionStatus)
                verify(sessionAuthenticationStrategy).onAuthentication(
                    authentication.capture(),
                    eq(request),
                    eq(response)
                )
                verify(securityContextRepository).saveContext(context.capture(), eq(request), eq(response))
            }
            assertSame(authentication.firstValue, context.firstValue.authentication)
            assertSame(SecurityContextHolder.getContext(), context.firstValue)
            verify(memberAccessor, never()).save(any<Member>())
        }

        @Test
        fun `처음 로그인하는 네이버 회원은 무료 플랜으로 회원가입함`() {
            /** given */
            whenever(naverOAuthAccessor.findByProviderId(providerId)).thenReturn(null)
            whenever(planAccessor.findByType(PlanType.FREE)).thenReturn(freePlan)
            whenever(memberAccessor.save(any<Member>())).thenReturn(member)

            /** when */
            val result = authService.signInWithNaverCallback(code, state, request, response)

            /** then */
            val oauthCaptor = argumentCaptor<NaverOAuth>()
            verify(naverOAuthAccessor).save(oauthCaptor.capture())
            assertEquals(providerId, oauthCaptor.firstValue.providerId)
            assertEquals(member, oauthCaptor.firstValue.member)
            assertEquals(tokenResponse.accessToken, oauthCaptor.firstValue.accessToken)
            assertEquals(tokenResponse.refreshToken, oauthCaptor.firstValue.refreshToken)
            assertEquals(31L, result.data.memberId)
            assertEquals(MemberRole.NORMAL.name, result.data.role)
            val savedMember = argumentCaptor<Member>()
            verify(memberAccessor).save(savedMember.capture())
            assertEquals(profileResponse.response.email, savedMember.firstValue.email)
            assertEquals(now, savedMember.firstValue.lastSignInAt)
            assertEquals(PlanType.FREE, savedMember.firstValue.plan.type)
            verify(member).ensureSignInAllowed()
            verify(member).completeSignIn(now)
        }

        @Test
        fun `로그인할 수 없는 회원은 로그인 시각을 갱신하지 않음`() {
            /** given */
            whenever(naverOAuthAccessor.findByProviderId(providerId))
                .thenReturn(NaverOAuth.from(member, profileResponse, tokenResponse))
            doThrow(BusinessException(AuthError.BANNED_ACCOUNT)).whenever(member).ensureSignInAllowed()

            /** when */
            val exception = assertFailsWith<BusinessException> {
                authService.signInWithNaverCallback(code, state, request, response)
            }

            /** then */
            assertEquals(AuthError.BANNED_ACCOUNT, exception.error)
            verify(member, never()).completeSignIn(any())
            verify(memberAccessor, never()).save(any<Member>())
        }

        @Test
        fun `무료 플랜이 준비되지 않으면 신규 회원이 가입할 수 없음`() {
            /** given */
            whenever(naverOAuthAccessor.findByProviderId(providerId)).thenReturn(null)
            whenever(planAccessor.findByType(PlanType.FREE)).thenReturn(null)

            /** when */
            val exception = assertFailsWith<BusinessException> {
                authService.signInWithNaverCallback(code, state, request, response)
            }

            /** then */
            assertEquals(CommonError.INTERNAL_SERVER_ERROR, exception.error)
            verifyNoInteractions(memberAccessor)
            verify(naverOAuthAccessor, never()).save(any<NaverOAuth>())
        }

        @Test
        fun `네이버 프로필을 가져오지 못하면 가입이나 로그인을 진행하지 않음`() {
            /** given */
            whenever(naverAuthClient.fetchProfile(tokenResponse.accessToken))
                .thenThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))

            /** when */
            val exception = assertFailsWith<BusinessException> {
                authService.signInWithNaverCallback(code, state, request, response)
            }

            /** then */
            assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
            verifyNoInteractions(naverOAuthAccessor, memberAccessor)
        }

        @Test
        fun `세션에 일치하는 로그인 요청이 없으면 네이버 인증을 진행하지 않음`() {
            /** given */
            whenever(authorizationRequestRepository.removeAuthorizationRequest(request, response)).thenReturn(null)

            /** when */
            val exception = assertFailsWith<BusinessException> {
                authService.signInWithNaverCallback(code, state, request, response)
            }

            /** then */
            assertEquals(AuthError.INVALID_OAUTH_STATE, exception.error)
            verifyNoInteractions(
                naverAuthClient,
                transactionManager,
                sessionAuthenticationStrategy,
                securityContextRepository
            )
        }

        @Test
        fun `로그인 변경 사항을 저장하지 못하면 세션 인증을 저장하지 않음`() {
            /** given */
            whenever(naverOAuthAccessor.findByProviderId(providerId))
                .thenReturn(NaverOAuth.from(member, profileResponse, tokenResponse))
            val commitException = IllegalStateException("transaction commit failed")
            doThrow(commitException).whenever(transactionManager).commit(transactionStatus)

            /** when */
            val exception = assertFailsWith<IllegalStateException> {
                authService.signInWithNaverCallback(code, state, request, response)
            }

            /** then */
            assertSame(commitException, exception)
            assertNull(SecurityContextHolder.getContext().authentication)
            verifyNoInteractions(sessionAuthenticationStrategy, securityContextRepository)
        }
    }
}
