package kr.byeongmin.watchtower.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class JwtProvider(
    @Value("\${watchtower.jwt.secret}") private val secret: String,
    @Value("\${watchtower.jwt.access-expiry}") val accessTokenExpiry: Long,
    @Value("\${watchtower.jwt.refresh-expiry}") val refreshTokenExpiry: Long
) {
    private val secretKey = Keys.hmacShaKeyFor(
        secret.toByteArray(StandardCharsets.UTF_8)
    )

    private fun getExpiry(tokenExpiry: Long): Date {
        return Date(Date().time + tokenExpiry)
    }

    fun createAccessToken(memberId: Long, role: String): String {
        return Jwts.builder()
            .subject(memberId.toString())
            .claim("role", role)
            .issuedAt(Date())
            .expiration(getExpiry(accessTokenExpiry))
            .signWith(secretKey)
            .compact()
    }

    fun createRefreshToken(memberId: Long): String {
        return Jwts.builder()
            .subject(memberId.toString())
            .issuedAt(Date())
            .expiration(getExpiry(refreshTokenExpiry))
            .signWith(secretKey)
            .compact()
    }

    fun isValidToken(token: String): Boolean {
        return runCatching {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
        }.isSuccess
    }

    fun validateToken(token: String) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
        } catch (e: ExpiredJwtException) {
            throw BusinessException(AuthError.EXPIRED_TOKEN)
        } catch (e: Exception) { // 보안 관점에서 뭉뚱그려 응답하면 공격이 피곤해짐
            throw BusinessException(AuthError.INVALID_TOKEN)
        }
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun getMemberId(token: String): Long {
        return getClaims(token)
            .subject
            .toLong()
    }

    fun getRole(token: String): String {
        return getClaims(token)
            .get("role", String::class.java)
    }
}