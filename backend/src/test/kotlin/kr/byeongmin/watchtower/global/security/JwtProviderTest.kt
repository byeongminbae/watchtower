package kr.byeongmin.watchtower.global.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class JwtProviderTest {
    private val secretKeyString = "충분히-긴-테스트용-비밀키-문자열-1234567890-가나다라마바사"
    private val jwtProvider = JwtProvider(
        secret = secretKeyString,
        accessTokenExpiry = 60_000L,
        refreshTokenExpiry = 600_000L
    )

    private val parser = Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8)))
        .build()

    @Test
    fun `액세스 토큰에는 발급 대상 회원의 ID와 권한이 담김`() {
        /** given */
        val memberId = 41L
        val role = "ADMIN"

        /** when */
        val token = jwtProvider.createAccessToken(memberId, role)

        /** then */
        assertTrue(jwtProvider.isValidToken(token))
        val claims = parser.parseSignedClaims(token).payload
        assertEquals(memberId.toString(), claims.subject)
        assertEquals(role, claims["role"])
    }

    @Test
    fun `리프레시 토큰에는 다시 인증할 회원의 ID가 담김`() {
        /** given */
        val memberId = 42L

        /** when */
        val token = jwtProvider.createRefreshToken(memberId)

        /** then */
        assertTrue(jwtProvider.isValidToken(token))
        assertEquals(memberId.toString(), parser.parseSignedClaims(token).payload.subject)
    }

    @Test
    fun `형식이 잘못됐거나 비어 있는 토큰은 사용할 수 없음`() {
        /** given */
        val memberId = 43L
        val role = "NORMAL"
        val token = jwtProvider.createAccessToken(memberId, role)

        /** when */
        val malformedHeaderValid = jwtProvider.isValidToken("변조된-$token")
        val malformedTokenValid = jwtProvider.isValidToken("토큰이-아닌-문자열")
        val emptyTokenValid = jwtProvider.isValidToken("")

        /** then */
        assertFalse(malformedHeaderValid)
        assertFalse(malformedTokenValid)
        assertFalse(emptyTokenValid)
    }

    @Test
    fun `만료된 토큰은 만료 오류로 거부함`() {
        /** given */
        val key = Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8))
        val memberId = 44L
        val expiredToken = Jwts.builder()
            .subject(memberId.toString())
            .issuedAt(Date(System.currentTimeMillis() - 2_000L))
            .expiration(Date(System.currentTimeMillis() - 1_000L))
            .signWith(key)
            .compact()

        /** when */
        val exception = assertFailsWith<BusinessException> {
            jwtProvider.validateToken(expiredToken)
        }

        /** then */
        assertEquals(AuthError.EXPIRED_TOKEN, exception.error)
    }

    @Test
    fun `서명이 변조된 토큰은 유효하지 않은 토큰으로 거부함`() {
        /** given */
        val token = jwtProvider.createAccessToken(43L, "NORMAL")
        val (header, payload, signature) = token.split('.')
        val tamperedSignature = (if (signature.first() == 'A') "B" else "A") + signature.drop(1)
        val tamperedToken = "$header.$payload.$tamperedSignature"

        /** when */
        val valid = jwtProvider.isValidToken(tamperedToken)
        val exception = assertFailsWith<BusinessException> {
            jwtProvider.validateToken(tamperedToken)
        }

        /** then */
        assertFalse(valid)
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
    }

    @Test
    fun `액세스 토큰과 리프레시 토큰에는 설정한 유효 기간이 기록됨`() {
        /** given */
        val memberId = 45L
        val role = "NORMAL"

        /** when */
        val before = System.currentTimeMillis()
        val accessToken = jwtProvider.createAccessToken(memberId, role)
        val refreshToken = jwtProvider.createRefreshToken(memberId)
        val after = System.currentTimeMillis()

        /** then */
        val accessClaims = parser.parseSignedClaims(accessToken).payload
        val refreshClaims = parser.parseSignedClaims(refreshToken).payload
        assertTrue(accessClaims.issuedAt.time in (before / 1000 * 1000)..after)
        assertTrue(refreshClaims.issuedAt.time in (before / 1000 * 1000)..after)
        assertTrue(accessClaims.expiration.time in ((before + 60_000L) / 1000 * 1000)..(after + 60_000L))
        assertTrue(refreshClaims.expiration.time in ((before + 600_000L) / 1000 * 1000)..(after + 600_000L))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "유효하지-않은-토큰"])
    fun `잘못된 토큰에서 회원 ID나 권한을 읽으려 하면 토큰 오류가 발생함`(token: String) {
        /** when */
        val memberIdException = assertFailsWith<BusinessException> {
            jwtProvider.getMemberId(token)
        }
        val roleException = assertFailsWith<BusinessException> {
            jwtProvider.getRole(token)
        }

        /** then */
        assertEquals(AuthError.INVALID_TOKEN, memberIdException.error)
        assertEquals(AuthError.INVALID_TOKEN, roleException.error)
    }

    @Test
    fun `만료된 토큰에서 회원 ID나 권한을 읽으려 하면 만료 오류가 발생함`() {
        /** given */
        val token = Jwts.builder()
            .subject("41")
            .claim("role", "NORMAL")
            .expiration(Date(0))
            .signWith(Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8)))
            .compact()

        /** when */
        val memberIdException = assertFailsWith<BusinessException> {
            jwtProvider.getMemberId(token)
        }
        val roleException = assertFailsWith<BusinessException> {
            jwtProvider.getRole(token)
        }

        /** then */
        assertEquals(AuthError.EXPIRED_TOKEN, memberIdException.error)
        assertEquals(AuthError.EXPIRED_TOKEN, roleException.error)
    }

    @Test
    fun `서명된 토큰의 회원 ID와 권한을 읽을 수 있음`() {
        /** given */
        val token = Jwts.builder()
            .subject("47")
            .claim("role", "ADMIN")
            .expiration(Date(System.currentTimeMillis() + 60_000L))
            .signWith(Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8)))
            .compact()

        /** when, then */
        assertEquals(47L, jwtProvider.getMemberId(token))
        assertEquals("ADMIN", jwtProvider.getRole(token))
    }
}
