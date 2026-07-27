package kr.byeongmin.watchtower.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.byeongmin.watchtower.domain.member.entity.Member
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class JwtProvider(
    val date: Date,
    @Value("\${watchtower.jwt.secret}") private val secret: String,
    @Value("\${watchtower.jwt.access-expiry}") private val accessTokenExpiry: Long,
    @Value("\${watchtower.jwt.refresh-expiry}") private val refreshTokenExpiry: Long
) {
    private val secretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun createAccessToken(member: Member): String {
        val currentDate = date
        val expiry = Date(date.time + accessTokenExpiry)

        return Jwts.builder()
            .subject(member.id.toString())
            .claim("role", member.role.name)
            .issuedAt(currentDate)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun createRefreshToken(member: Member): String {
        val currentDate = date
        val expiry = Date(date.time + accessTokenExpiry)

        return Jwts.builder()
            .subject(member.id.toString())
            .issuedAt(currentDate)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun getAccessTokenExpiry() = accessTokenExpiry

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getMemberId(token: String): Long {
        return getClaims(token).subject.toLong()
    }

    fun getRole(token: String): String {
        return getClaims(token)
            .get("role", String::class.java)
    }
}