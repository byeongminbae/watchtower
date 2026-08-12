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
    fun `만료된 액세스 토큰이 전달된 상황에서 요청을 처리하면 만료 토큰 오류 응답을 반환하고 다음 필터를 호출하지 않는다`() {
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
    fun `정상 요청이 들어온 상황에서 요청을 처리하면 다음 필터에 처리를 위임한다`() {
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
    fun `필터 체인에서 일반 예외가 발생한 상황에서 요청을 처리하면 예외를 그대로 던진다`() {
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
    fun `인증 토큰이 없는 상황에서 인증 진입점을 호출하면 인증 실패 응답을 반환한다`() {
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
    fun `접근 거부 예외가 주어진 상황에서 접근 거부 핸들러가 예외를 처리하면 접근 거부 응답을 반환한다`() {
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
