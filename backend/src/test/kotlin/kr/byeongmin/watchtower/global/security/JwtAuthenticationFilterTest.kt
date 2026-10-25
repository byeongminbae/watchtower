package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import kotlin.test.*
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest {
    private val jwtProvider = mock<JwtProvider>()
    private val filter = JwtAuthenticationFilter(jwtProvider)

    @AfterEach
    fun `시큐리티 컨텍스트 비움`() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `유효한 액세스 토큰을 보낸 회원은 토큰에 담긴 ID와 권한으로 인증됨`() {
        /** given */
        val accessToken = "타워타워-엑세스-토큰"
        val role = "ADMIN"
        val memberId = 51L
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $accessToken")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        whenever(jwtProvider.getMemberId(accessToken)).thenReturn(memberId)
        whenever(jwtProvider.getRole(accessToken)).thenReturn(role)

        /** when */
        filter.doFilter(request, response, chain)

        /** then */
        val authentication = assertNotNull(SecurityContextHolder.getContext().authentication)
        val principal = assertIs<MemberPrinciple>(authentication.principal)
        assertEquals(memberId, principal.memberId)
        assertEquals(role, principal.role)
        assertTrue(authentication.authorities.any { it.authority == "ROLE_$role" })
        verify(jwtProvider).validateToken(accessToken)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `인증 헤더가 없는 요청은 인증 없이 계속 처리함`() {
        /** given */
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        /** when */
        filter.doFilter(request, response, chain)

        /** then */
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtProvider, never()).validateToken(any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `Bearer 토큰이 아닌 인증 정보는 사용하지 않고 요청을 계속 처리함`() {
        /** given */
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Basic 우린-베이직-안써용")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        /** when */
        filter.doFilter(request, response, chain)

        /** then */
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtProvider, never()).validateToken(any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `유효하지 않은 토큰을 보낸 요청은 토큰 오류와 함께 처리를 중단함`() {
        /** given */
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer 유효하지-않은-타워타워-엑세스-토큰")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        val exception = BusinessException(AuthError.INVALID_TOKEN)
        doThrow(exception).whenever(jwtProvider).validateToken("유효하지-않은-타워타워-엑세스-토큰")

        /** when */
        val thrown = assertFailsWith<BusinessException> {
            filter.doFilter(request, response, chain)
        }

        /** then */
        assertSame(exception, thrown)
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `토큰에서 회원 정보를 읽지 못하면 인증 없이 요청을 계속 처리함`() {
        /** given */
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer 추출에-실패하는-타워타워-엑세스-토큰")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        whenever(jwtProvider.getMemberId("추출에-실패하는-타워타워-엑세스-토큰"))
            .thenThrow(NumberFormatException())

        /** when */
        filter.doFilter(request, response, chain)

        /** then */
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtProvider).validateToken("추출에-실패하는-타워타워-엑세스-토큰")
        verify(chain).doFilter(request, response)
    }
}
