package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import kr.byeongmin.watchtower.global.error.AuthError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import tools.jackson.databind.json.JsonMapper
import kotlin.test.*

class JwtAuthenticationFilterTest {
    private val jwtProvider = mock<JwtProvider>()
    private val filter = JwtAuthenticationFilter(jwtProvider, JsonMapper.builder().build())

    @AfterEach
    fun `시큐리티 컨텍스트 비우기(쓰레드 로컬 공유 이슈)`() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `엑세스 토큰이 유효할 경우 시큐리티 컨텍스트에 인증 정보 저장`() {
        // Given
        val accessToken = "와치타워-엑세스-토큰"
        val role = "ADMIN"
        val memberId = 51L
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $accessToken")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        whenever(jwtProvider.isValidToken(accessToken)).thenReturn(true)
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
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `인증 헤더가 비어있는 경우`() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        // When
        filter.doFilter(request, response, chain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(jwtProvider, never()).isValidToken(any())
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `지원하지 않는 전달자 형식을 사용한 경우`() {
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
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `유효하지 않은 토큰을 사용한 경우`() {
        // Given
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer 유효하지-않은-와치타워-엑세스-토큰")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        whenever(jwtProvider.isValidToken("유효하지-않은-와치타워-엑세스-토큰")).thenReturn(false)

        // When
        filter.doFilter(request, response, chain)

        // Then
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        assertTrue(response.contentAsString.contains(AuthError.INVALID_TOKEN.statusCode))
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `토큰 상세 정보 추출에 실패한 경우`() {
        // Given
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer 추출에-실패하는-와치타워-엑세스-토큰")
        }
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        whenever(jwtProvider.isValidToken("추출에-실패하는-와치타워-엑세스-토큰")).thenReturn(true)
        whenever(jwtProvider.getMemberId("추출에-실패하는-와치타워-엑세스-토큰"))
            .thenThrow(NumberFormatException())

        // When
        filter.doFilter(request, response, chain)

        // Then
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }
}
