package kr.byeongmin.watchtower.global.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.nio.charset.StandardCharsets
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtAuthenticationFilterTests {
    private val jwtProvider = JwtProvider(
        date = Date(),
        secret = SECRET,
        accessTokenExpiry = 3_600_000,
        refreshTokenExpiry = 7_200_000,
    )
    private val filter = JwtAuthenticationFilter(jwtProvider)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid bearer token creates Spring Security authentication`() {
        val request = MockHttpServletRequest().apply {
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer ${accessToken(42, "USER")}")
        }

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        val authentication = assertNotNull(SecurityContextHolder.getContext().authentication)
        assertEquals(WatchtowerPrincipal(42, "USER"), authentication.principal)
        assertEquals(listOf("ROLE_USER"), authentication.authorities.map { it.authority })
    }

    @Test
    fun `invalid bearer token remains anonymous`() {
        val request = MockHttpServletRequest().apply {
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        }

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    private fun accessToken(memberId: Long, role: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(memberId.toString())
            .claim("role", role)
            .issuedAt(now)
            .expiration(Date(now.time + 60_000))
            .signWith(Keys.hmacShaKeyFor(SECRET.toByteArray(StandardCharsets.UTF_8)))
            .compact()
    }

    private companion object {
        const val SECRET = "watchtower-test-secret-key-32-bytes-long"
    }
}
