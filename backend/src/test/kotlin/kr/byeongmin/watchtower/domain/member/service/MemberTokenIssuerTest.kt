package kr.byeongmin.watchtower.domain.member.service

import java.time.LocalDateTime
import kotlin.test.assertEquals
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.security.JwtProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class MemberTokenIssuerTest {
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }
    private val jwtProvider = mock<JwtProvider>()
    private val member = mock<Member> {
        on { id } doReturn 11L
        on { role } doReturn MemberRole.NORMAL
    }
    private val issuer = MemberTokenIssuer(jwtProvider, timeProvider)

    @BeforeEach
    fun setUp() {
        whenever(jwtProvider.createAccessToken(11L, "NORMAL")).thenReturn("access")
        whenever(jwtProvider.createRefreshToken(11L)).thenReturn("refresh")
        whenever(jwtProvider.accessTokenExpiry).thenReturn(60_000L)
        whenever(jwtProvider.refreshTokenExpiry).thenReturn(600_000L)
    }

    @Test
    fun `로그인하면 토큰을 발급하고 회원에게 로그인한 시각과 리프레시 토큰을 반영함`() {
        /** when */
        val response = issuer.signIn(member)

        /** then */
        assertEquals("access", response.accessToken)
        assertEquals("refresh", response.refreshToken)
        assertEquals(60_000L, response.accessTokenExpiresIn)
        assertEquals(600_000L, response.refreshTokenExpiresIn)
        verify(member).completeSignIn("refresh", now)
    }

    @Test
    fun `토큰을 다시 발급하면 회원의 리프레시 토큰과 최근 로그인 시각도 갱신함`() {
        /** when */
        val response = issuer.rotate(member)

        /** then */
        assertEquals("access", response.accessToken)
        assertEquals("refresh", response.refreshToken)
        assertEquals(60_000L, response.accessTokenExpiresIn)
        assertEquals(600_000L, response.refreshTokenExpiresIn)
        verify(member).rotateRefreshToken("refresh", now)
    }

}
