package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authorization 헤더의 Bearer 토큰을 검증하고 회원 식별자와 단일 권한을 인증 컨텍스트에 설정하는 필터
 *
 * [kr.byeongmin.watchtower.domain.member.entity.Member] 의 role 이 여러개로 늘어나면 이 클래스도 바뀌어야함
 * */
@Component
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {
    /** 토큰이 없으면 그대로 진행하고, 검증 실패는 다음 필터로 전달하며, 클레임 추출 실패는 인증 설정을 생략한다. */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = getAuthorizationToken(request)

        if (!token.isNullOrBlank()) {
            jwtProvider.validateToken(token)

            createUsernamePasswordAuthenticationToken(token)?.let { authentication ->
                SecurityContextHolder.getContext().authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getAuthorizationToken(request: HttpServletRequest): String? {
        return request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
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
                listOf(SimpleGrantedAuthority("ROLE_$role"))
            )
        }.getOrNull()
    }
}
