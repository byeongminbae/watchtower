package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.isNotNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 정지 또는 탈퇴된 회원의 기존 로그인 차단 및 변경된 회원 권한 즉시 반영용 필터
 */
class SessionAuthenticationFilter(
    private val sessionMemberLoader: SessionMemberLoader,
    private val securityContextLogoutHandler: SecurityContextLogoutHandler
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? MemberPrinciple
        if (principal.isNotNull()) {
            val currentMember = try {
                sessionMemberLoader.getMemberPrinciple(principal.memberId)
            } catch (exception: BusinessException) {
                when (exception.error) {
                    AuthError.UNAUTHORIZED,
                    AuthError.ALREADY_DELETED_ACCOUNT,
                    AuthError.ACCOUNT_NOT_FOUND,
                    AuthError.BANNED_ACCOUNT -> securityContextLogoutHandler.logout(
                        request,
                        response,
                        SecurityContextHolder.getContext().authentication
                    )
                    else -> {}
                }
                throw exception
            }
            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = currentMember.to()
            SecurityContextHolder.setContext(context)
        }
        filterChain.doFilter(request, response)
    }
}
