package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.utils.sendErrorResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    private fun getAuthorizationToken(request: HttpServletRequest): String? {
        return request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = getAuthorizationToken(request)

        if (!token.isNullOrBlank()) {
            if (!jwtProvider.isValidToken(token)) {
                response.sendErrorResponse(objectMapper, AuthError.INVALID_TOKEN)
                return
            }

            createUsernamePasswordAuthenticationToken(token)?.let { authentication ->
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun createUsernamePasswordAuthenticationToken(token: String): UsernamePasswordAuthenticationToken? {
        return runCatching {
            val memberId = jwtProvider.getMemberId(token)
            val role = jwtProvider.getRole(token)

            UsernamePasswordAuthenticationToken(
                MemberPrinciple(
                    memberId = memberId,
                    role = role
                ),
                null,
                // TODO: 인당 권한 여러개 들어가면 수정해야함
                listOf(SimpleGrantedAuthority("ROLE_$role")),
            )
        }.getOrNull()
    }
}
