package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SecurityExceptionHandlerFilterTest {
    private val objectMapper = jacksonObjectMapper()
    private val filter = SecurityExceptionHandlerFilter(objectMapper)

    @Test
    fun `JWT 필터에서 발생한 비즈니스 예외를 오류 응답으로 변환`() {
        // Given
        val accessToken = "만료된-와치타워-액세스-토큰"
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer $accessToken")
        }
        val response = MockHttpServletResponse()
        val jwtProvider = mock<JwtProvider>()
        val jwtAuthenticationFilter = JwtAuthenticationFilter(jwtProvider)
        val downstreamChain = mock<FilterChain>()
        val exception = BusinessException(
            AuthError.EXPIRED_TOKEN,
            LocalDateTime.of(2026, 8, 2, 12, 34, 56),
        )
        doThrow(exception).`when`(jwtProvider).validateToken(accessToken)
        val jwtFilterChain = FilterChain { servletRequest, servletResponse ->
            jwtAuthenticationFilter.doFilter(servletRequest, servletResponse, downstreamChain)
        }

        // When
        filter.doFilter(request, response, jwtFilterChain)

        // Then
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        assertEquals("UTF-8", response.characterEncoding)
        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(false, body["success"].booleanValue())
        assertEquals(AuthError.EXPIRED_TOKEN.statusCode, body["statusCode"].stringValue())
        assertEquals(AuthError.EXPIRED_TOKEN.message, body["message"].stringValue())
        assertEquals("2026-08-02T12:34:56", body["timestamp"].stringValue())
        verifyNoInteractions(downstreamChain)
    }

    @Test
    fun `정상 요청은 필터 체인으로 위임`() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        // When
        filter.doFilter(request, response, chain)

        // Then
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `비즈니스 예외가 아닌 예외는 다시 던짐`() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        val exception = IllegalStateException("예상하지 못한 예외")
        doThrow(exception).`when`(chain).doFilter(request, response)

        // When
        val thrown = assertFailsWith<IllegalStateException> {
            filter.doFilter(request, response, chain)
        }

        // Then
        assertEquals(exception, thrown)
    }

    @Test
    fun `토큰 없이 인증이 필요한 컨트롤러에 접근한 경우`() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        // When
        filter.commence(request, response, AuthenticationCredentialsNotFoundException("인증 정보 없음"))

        // Then
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        assertTrue(response.contentAsString.contains("\"statusCode\":\"${AuthError.UNAUTHORIZED.statusCode}\""))
    }

    @Test
    fun `회원의 role이 컨트롤러에서 요구되는 role 미만인 경우`() {
        // Given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        // When
        filter.handle(request, response, AccessDeniedException("접근 거부"))

        // Then
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        assertTrue(response.contentAsString.contains("\"statusCode\":\"${AuthError.ACCESS_DENIED.statusCode}\""))
    }
}
