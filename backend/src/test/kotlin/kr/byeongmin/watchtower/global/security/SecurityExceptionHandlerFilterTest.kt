package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SecurityExceptionHandlerFilterTest {
    private val objectMapper = jacksonObjectMapper()
    private val filter = SecurityExceptionHandlerFilter(objectMapper)

    @Test
    fun `후속 처리에서 발생한 비즈니스 예외를 응답으로 변환함`() {
        /** given */
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        doThrow(
            BusinessException(
                AuthError.UNAUTHORIZED,
                LocalDateTime.of(2026, 8, 2, 12, 34, 56)
            )
        ).`when`(chain).doFilter(request, response)

        /** when */
        filter.doFilter(request, response, chain)

        /** then */
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(false, assertNotNull(body["isSuccess"], response.contentAsString).booleanValue())
        assertEquals(AuthError.UNAUTHORIZED.statusCode, body["statusCode"].stringValue())
    }

    @Test
    fun `인증 오류가 없는 요청은 그대로 처리를 이어감`() {
        /** given */
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()

        /** when */
        filter.doFilter(request, response, chain)

        /** then */
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `일반 예외는 인증 오류 응답으로 바꾸지 않고 그대로 전파함`() {
        /** given */
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = mock<FilterChain>()
        val exception = IllegalStateException("예상하지 못한 예외")
        doThrow(exception).`when`(chain).doFilter(request, response)

        /** when */
        val thrown = assertFailsWith<IllegalStateException> {
            filter.doFilter(request, response, chain)
        }

        /** then */
        assertEquals(exception, thrown)
    }

    @Test
    fun `인증 정보가 없으면 인증이 필요하다는 오류를 응답함`() {
        /** given */
        val response = MockHttpServletResponse()

        /** when */
        filter.commence(MockHttpServletRequest(), response, AuthenticationCredentialsNotFoundException("인증 정보 없음"))

        /** then */
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        assertEquals(
            AuthError.UNAUTHORIZED.statusCode,
            objectMapper.readTree(response.contentAsString)["statusCode"].stringValue()
        )
    }

    @Test
    fun `접근이 거부되면 권한이 없다는 오류를 응답함`() {
        /** given */
        val response = MockHttpServletResponse()

        /** when */
        filter.handle(MockHttpServletRequest(), response, AccessDeniedException("접근 거부"))

        /** then */
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        assertEquals(
            AuthError.ACCESS_DENIED.statusCode,
            objectMapper.readTree(response.contentAsString)["statusCode"].stringValue()
        )
    }
}
