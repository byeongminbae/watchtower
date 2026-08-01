package kr.byeongmin.watchtower.global.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtProviderTest {
    private val secretKeyString = "충분히-긴-테스트용-비밀키-문자열-1234567890-가나다라마바사"
    private val jwtProvider = JwtProvider(
        secret = secretKeyString,
        accessTokenExpiry = 1_000L,
        refreshTokenExpiry = 10_000L,
    )

    @Test
    fun `회원 엑세스 토큰 발급`() {
        // Given
        val memberId = 41L
        val role = "ADMIN"

        // When
        val token = jwtProvider.createAccessToken(memberId, role)

        // Then
        assertTrue(jwtProvider.isValidToken(token))
        assertEquals(memberId, jwtProvider.getMemberId(token))
        assertEquals(role, jwtProvider.getRole(token))
    }

    @Test
    fun `회원 리프레시 토큰 발급`() {
        // Given
        val memberId = 42L

        // When
        val token = jwtProvider.createRefreshToken(memberId)

        // Then
        assertTrue(jwtProvider.isValidToken(token))
        assertEquals(memberId, jwtProvider.getMemberId(token))
    }

    @Test
    fun `토큰이 유효하지 않을 경우`() {
        // Given
        val memberId = 43L
        val role = "NORMAL"

        // When
        val token = jwtProvider.createAccessToken(memberId, role)

        // Then
        assertFalse(jwtProvider.isValidToken("변조된-$token"))
        assertFalse(jwtProvider.isValidToken("토큰이-아닌-문자열"))
        assertFalse(jwtProvider.isValidToken(""))
    }

    @Test
    fun `만료된 토큰은 만료 예외를 던짐`() {
        // Given
        val key = Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8))
        val memberId = 44L
        val expiredToken = Jwts.builder()
            .subject(memberId.toString())
            .issuedAt(Date(System.currentTimeMillis() - 2_000L))
            .expiration(Date(System.currentTimeMillis() - 1_000L))
            .signWith(key)
            .compact()

        // When
        val exception = assertFailsWith<BusinessException> {
            jwtProvider.validateToken(expiredToken)
        }

        // Then
        assertEquals(AuthError.EXPIRED_TOKEN, exception.error)
    }

    @Test
    fun `변조된 토큰은 유효하지 않은 토큰 예외를 던짐`() {
        // When
        val exception = assertFailsWith<BusinessException> {
            jwtProvider.validateToken("유효하지-않음")
        }

        // Then
        assertEquals(AuthError.INVALID_TOKEN, exception.error)
    }

    @Test
    fun `엑세스 토큰과 리프레시 토큰은 각각 설정된 수명을 사용`() {
        // Given
        val memberId = 45L
        val role = "NORMAL"
        val accessClaims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(jwtProvider.createAccessToken(memberId, role))
            .payload
        val refreshClaims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secretKeyString.toByteArray(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(jwtProvider.createRefreshToken(memberId))
            .payload

        // When
        val accessTokenLifetime = accessClaims.expiration.time - accessClaims.issuedAt.time
        val refreshTokenLifetime = refreshClaims.expiration.time - refreshClaims.issuedAt.time

        // Then
        assertEquals(1_000L, accessTokenLifetime)
        assertEquals(10_000L, refreshTokenLifetime)
    }
}
