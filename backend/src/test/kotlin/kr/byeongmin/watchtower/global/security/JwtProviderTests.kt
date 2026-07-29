package kr.byeongmin.watchtower.global.security

import org.junit.jupiter.api.Test
import java.util.Date
import kotlin.test.assertEquals

class JwtProviderTests {
    private val jwtProvider = JwtProvider(
        date = Date(),
        secret = "watchtower-test-secret-key-32-bytes-long",
        accessTokenExpiry = 60_000,
        refreshTokenExpiry = 120_000,
    )

    @Test
    fun `access token contains security-neutral member claims`() {
        val token = jwtProvider.createAccessToken(42, "ADMIN")

        assertEquals(42, jwtProvider.getMemberId(token))
        assertEquals("ADMIN", jwtProvider.getRole(token))
    }

    @Test
    fun `refresh token contains member id`() {
        val token = jwtProvider.createRefreshToken(42)

        assertEquals(42, jwtProvider.getMemberId(token))
    }
}
