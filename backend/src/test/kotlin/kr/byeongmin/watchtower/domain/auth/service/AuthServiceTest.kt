package kr.byeongmin.watchtower.domain.auth.service

import kr.byeongmin.watchtower.domain.auth.accessor.NaverOAuthAccessor
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.service.MemberTokenIssuer
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
import kr.byeongmin.watchtower.global.security.JwtProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthServiceTest {
    private val memberAccessor = mock<MemberAccessor>()
    private val naverOAuthAccessor = mock<NaverOAuthAccessor>()
    private val planAccessor = mock<PlanAccessor>()
    private val memberTokenIssuer = mock<MemberTokenIssuer>()
    private val jwtProvider = mock<JwtProvider>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }

    private val naverAuthClient = mock<NaverAuthClient>()
    private val freePlan = mock<Plan> {
        on { type } doReturn PlanType.FREE
    }
    private val authService = AuthService(
        naverAuthClient = naverAuthClient,
        memberAccessor = memberAccessor,
        naverOAuthAccessor = naverOAuthAccessor,
        planAccessor = planAccessor,
        memberTokenIssuer = memberTokenIssuer,
        jwtProvider = jwtProvider,
        timeProvider = timeProvider
    )

    @Test
    fun `네이버 로그인에 사용할 인증 URL을 받을 수 있음`() {
        /** given */
        val state = "state"
        val signInUrl = "네이버-인증-클라이언트가-생성한-로그인-URL"
        whenever(naverAuthClient.buildSignInUrl(state)).thenReturn(signInUrl)

        /** when */
        val response = authService.getNaverSignInUrl(state)

        /** then */
        assertEquals(signInUrl, response.data)
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

        private val member = mock<Member>()
        private val providerId = profileResponse.response.providerId
        private val watchtowerMemberToken = MemberTokenResponseDto(
            accessToken = "access",
            refreshToken = "refresh",
            accessTokenExpiresIn = 3600L,
            refreshTokenExpiresIn = 10000L
        )
        private val code = "code"
        private val state = "state"

        @BeforeEach
        fun setUpNaverApiStubs() {
            whenever(naverAuthClient.fetchToken(code, state)).thenReturn(tokenResponse)

            whenever(naverAuthClient.fetchProfile(tokenResponse.accessToken)).thenReturn(profileResponse)
        }

        @Test
        fun `이미 가입한 네이버 회원은 다시 가입하지 않고 로그인할 수 있음`() {
            /** given */
            val naverOAuth = NaverOAuth.from(
                member = member,
                naverProfileResponseDto = profileResponse,
                naverTokenResponseDto = tokenResponse
            )
            whenever(naverOAuthAccessor.findByProviderId(providerId)).thenReturn(naverOAuth)
            whenever(memberTokenIssuer.signIn(member)).thenReturn(watchtowerMemberToken)

            /** when */
            val response = authService.signInWithNaverCallback(code, state)

            /** then */
            assertEquals(expected = watchtowerMemberToken, actual = response.data)
            verify(memberAccessor, never()).save(any<Member>())
        }

        @Test
        fun `처음 로그인하는 네이버 회원은 무료 플랜으로 회원가입함`() {
            /** given */
            whenever(naverOAuthAccessor.findByProviderId(providerId)).thenReturn(null)
            whenever(planAccessor.findByType(PlanType.FREE)).thenReturn(freePlan)
            whenever(memberAccessor.save(any<Member>())).thenReturn(member)
            whenever(memberTokenIssuer.signIn(member)).thenReturn(watchtowerMemberToken)

            /** when */
            val response = authService.signInWithNaverCallback(code, state)

            /** then */
            val oauthCaptor = argumentCaptor<NaverOAuth>()
            verify(naverOAuthAccessor).save(oauthCaptor.capture())
            assertEquals(providerId, oauthCaptor.firstValue.providerId)
            assertEquals(member, oauthCaptor.firstValue.member)
            assertEquals(tokenResponse.accessToken, oauthCaptor.firstValue.accessToken)
            assertEquals(tokenResponse.refreshToken, oauthCaptor.firstValue.refreshToken)
            assertEquals(watchtowerMemberToken, response.data)
            verify(naverOAuthAccessor).findByProviderId(providerId)
            val savedMember = argumentCaptor<Member>()
            verify(memberAccessor).save(savedMember.capture())
            assertEquals(profileResponse.response.email, savedMember.firstValue.email)
            assertEquals(now, savedMember.firstValue.lastSignInAt)
            assertEquals(PlanType.FREE, savedMember.firstValue.plan.type)
        }

        @Test
        fun `무료 플랜이 준비되지 않으면 신규 회원이 가입할 수 없음`() {
            /** given */
            whenever(naverOAuthAccessor.findByProviderId(providerId)).thenReturn(null)
            whenever(planAccessor.findByType(PlanType.FREE)).thenReturn(null)

            /** when */
            val exception = assertFailsWith<BusinessException> {
                authService.signInWithNaverCallback(code, state)
            }

            /** then */
            assertEquals(CommonError.INTERNAL_SERVER_ERROR, exception.error)
            verifyNoInteractions(memberAccessor, memberTokenIssuer)
            verify(naverOAuthAccessor, never()).save(any<NaverOAuth>())
        }

        @Test
        fun `네이버 프로필을 가져오지 못하면 가입이나 로그인을 진행하지 않음`() {
            /** given */
            whenever(naverAuthClient.fetchProfile(tokenResponse.accessToken))
                .thenThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))

            /** when */
            val exception = assertFailsWith<BusinessException> {
                authService.signInWithNaverCallback(code, state)
            }

            /** then */
            assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
            verifyNoInteractions(
                naverOAuthAccessor,
                memberAccessor,
                memberTokenIssuer
            )
        }
    }

    @Test
    fun `리프레시 토큰으로 회원의 토큰을 새로 발급받을 수 있음`() {
        /** given */
        val memberId = 31L
        val member = mock<Member>()
        val newMemberTokenResponse = MemberTokenResponseDto(
            accessToken = "new-access",
            refreshToken = "new-refresh",
            accessTokenExpiresIn = 3600L,
            refreshTokenExpiresIn = 10000L
        )
        whenever(jwtProvider.getMemberId("refresh")).thenReturn(memberId)
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(memberTokenIssuer.rotate(member)).thenReturn(newMemberTokenResponse)

        /** when */
        val response = authService.renewToken("refresh")

        /** then */
        assertEquals(newMemberTokenResponse, response.data)
    }

    @Test
    fun `존재하지 않는 회원에게 토큰을 다시 발급할 수 없음`() {
        /** given */
        val memberId = 31L
        whenever(jwtProvider.getMemberId("deleted-member-refresh")).thenReturn(memberId)
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            authService.renewToken("deleted-member-refresh")
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verifyNoInteractions(memberTokenIssuer)
    }

    @Test
    fun `로그아웃을 요청하면 조회한 회원에게 로그아웃을 반영함`() {
        /** given */
        val memberId = 31L
        val member = mock<Member>()
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)

        /** when */
        val response = authService.logout(memberId)

        /** then */
        assertTrue(response.isSuccess)
        verify(member).signOut()
    }

    @Test
    fun `존재하지 않는 회원은 로그아웃할 수 없음`() {
        /** given */
        val memberId = 31L
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            authService.logout(memberId)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `유효하지 않은 리프레시 토큰으로는 새 토큰을 발급하지 않음`() {
        /** given */
        whenever(jwtProvider.getMemberId("만료된-토큰"))
            .thenThrow(BusinessException(AuthError.EXPIRED_TOKEN))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            authService.renewToken("만료된-토큰")
        }

        /** then */
        assertEquals(AuthError.EXPIRED_TOKEN, exception.error)
        verifyNoInteractions(memberAccessor, memberTokenIssuer)
    }
}
