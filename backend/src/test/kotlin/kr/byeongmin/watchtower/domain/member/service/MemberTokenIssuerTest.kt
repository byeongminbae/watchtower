package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.security.JwtProvider
import kr.byeongmin.watchtower.support.MemberSupport
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MemberTokenIssuerTest {
    private val memberSupport = MemberSupport()
    private val jwtProvider = mock<JwtProvider>()
    private val memberTokenIssuer = MemberTokenIssuer(jwtProvider)

    private val naverProfile = NaverProfileResponseDto(
        resultCode = "00",
        message = "성공",
        response = NaverProfileResponseDto.NaverProfileDetailDto(
            providerId = "네이버-고객-고유-아이디",
            email = "네이버-고객-이메일",
            nickname = "네이버-고객-닉네임",
            profileImage = "https://naver.com/user/123/profile.png",
        ),
    )

    @Test
    fun `영속된 회원인 상황에서 로그인을 처리하면 토큰을 발급하고 리프레시 토큰을 저장한다`() {
        // Given
        val memberId = 11L
        val accessToken = "와치타워-엑세스-토큰"
        val refreshToken = "와치타워-리프레시-토큰"
        val role = "NORMAL"
        val accessTokenExpiry = 600L
        val member = memberSupport.createMember(naverProfile, memberId)
        whenever(jwtProvider.createAccessToken(memberId, role)).thenReturn(accessToken)
        whenever(jwtProvider.createRefreshToken(memberId)).thenReturn(refreshToken)
        whenever(jwtProvider.accessTokenExpiry).thenReturn(accessTokenExpiry)

        // When
        val response = memberTokenIssuer.signIn(member)

        // Then
        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
        assertEquals(accessTokenExpiry, response.accessTokenExpiry)
        assertEquals(refreshToken, member.refreshToken)
        verify(jwtProvider).createAccessToken(memberId, role)
        verify(jwtProvider).createRefreshToken(memberId)
    }

    @Test
    fun `기존 리프레시 토큰이 있는 회원인 상황에서 토큰을 갱신하면 새 토큰으로 교체한다`() {
        // Given
        val memberId = 12L
        val accessToken = "새로운-와치타워-엑세스-토큰"
        val refreshToken = "새로운-와치타워-리프레시-토큰"
        val role = "NORMAL"
        val accessTokenExpiry = 300L
        val refreshTokenExpiry = 10_000L
        val member = memberSupport.createMember(naverProfile, memberId)
        member.rotateRefreshToken("기존-와치타워-리프레시-토큰")
        whenever(jwtProvider.createAccessToken(memberId, role)).thenReturn(accessToken)
        whenever(jwtProvider.createRefreshToken(memberId)).thenReturn(refreshToken)
        whenever(jwtProvider.accessTokenExpiry).thenReturn(accessTokenExpiry)
        whenever(jwtProvider.refreshTokenExpiry).thenReturn(refreshTokenExpiry)

        // When
        val response = memberTokenIssuer.rotate(member)

        // Then
        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
        assertEquals(accessTokenExpiry, response.accessTokenExpiry)
        assertEquals(refreshTokenExpiry, response.refreshTokenExpiry)
        assertEquals(refreshToken, member.refreshToken)
        verify(jwtProvider).createAccessToken(memberId, role)
        verify(jwtProvider).createRefreshToken(memberId)
    }

    @Test
    fun `영속되지 않은 회원인 상황에서 로그인을 처리하면 널 캐스팅 예외를 던진다`() {
        // Given
        val member = Member.from(naverProfile)

        // When
        val exception = assertFailsWith<BusinessException> {
            memberTokenIssuer.signIn(member)
        }

        // Then
        assertEquals(CommonError.NULL_CASTING_ERROR, exception.error)
        assertNull(member.refreshToken)
        verifyNoInteractions(jwtProvider)
    }
}
