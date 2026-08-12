package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.*

class JwtAuthenticationFilterTest {
    private val jwtProvider = mock<JwtProvider>()
    private val filter = JwtAuthenticationFilter(jwtProvider)

    @AfterEach
    fun `시큐리티 컨텍스트 비우기(쓰레드 로컬 공유 이슈)`() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `유효한 액세스 토큰이 전달된 상황에서 인증 필터를 실행하면 시큐리티 컨텍스트에 회원 인증 정보를 저장한다`() {
        // Given
        val accessToken = "와치타워-엑세스-토큰"
        val role = "ADMIN"
        val memberId = 51L
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $accessToken")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        whenever(jwtProvider.getMemberId(accessToken)).thenReturn(memberId)
        whenever(jwtProvider.getRole(accessToken)).thenReturn(role)

        // When
        filter.doFilter(request, response, chain)

        // Then
        val authentication = assertNotNull(SecurityContextHolder.getContext().authentication)
        val principal = assertIs<MemberPrinciple>(authentication.principal)
        assertEquals(memberId, principal.memberId)
        assertEquals(role, principal.role)
        assertTrue(authentication.authorities.any { it.authority == "ROLE_$role" })
        verify(jwtProvider).validateToken(accessToken)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `인증 헤더가 없는 상황에서 인증 필터를 실행하면 인증 없이 필터 체인에 위임한다`() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        // When
        filter.doFilter(request, response, chain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtProvider, never()).validateToken(any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `Bearer가 아닌 인증 형식이 전달된 상황에서 인증 필터를 실행하면 인증 없이 필터 체인에 위임한다`() {
        // Given
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Basic 우린-베이직-안써용")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        // When
        filter.doFilter(request, response, chain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtProvider, never()).validateToken(any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `유효하지 않은 액세스 토큰이 전달된 상황에서 토큰 검증에 실패하면 예외를 던지고 필터 체인 호출을 차단한다`() {
        // Given
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer 유효하지-않은-와치타워-엑세스-토큰")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        val exception = BusinessException(AuthError.INVALID_TOKEN)
        doThrow(exception).whenever(jwtProvider).validateToken("유효하지-않은-와치타워-엑세스-토큰")

        // When
        val thrown = assertFailsWith<BusinessException> {
            filter.doFilter(request, response, chain)
        }

        // Then
        assertSame(exception, thrown)
        assertEquals(200, response.status)
        assertNull(response.contentType)
        assertEquals("", response.contentAsString)
        verify(jwtProvider).validateToken("유효하지-않은-와치타워-엑세스-토큰")
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `토큰 상세 정보 추출이 실패하는 상황에서 인증 필터를 실행하면 인증 없이 필터 체인에 위임한다`() {
        // Given
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer 추출에-실패하는-와치타워-엑세스-토큰")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        whenever(jwtProvider.getMemberId("추출에-실패하는-와치타워-엑세스-토큰"))
            .thenThrow(NumberFormatException())

        // When
        filter.doFilter(request, response, chain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtProvider).validateToken("추출에-실패하는-와치타워-엑세스-토큰")
        verify(chain).doFilter(request, response)
    }
}
