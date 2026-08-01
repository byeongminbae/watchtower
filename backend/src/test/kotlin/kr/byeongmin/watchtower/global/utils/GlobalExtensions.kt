package kr.byeongmin.watchtower.global.utils

import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GlobalExtensions {
    @Test
    fun `널이 아닌 값은 객체를 그대로 반환`() {
        // Given
        val value = mutableListOf("값")

        // When
        val result = value.ifNullThrow()

        // Then
        assertSame(value, result)
    }

    @Test
    fun `널 값은 널 캐스팅 예외를 던짐`() {
        // Given
        val value: String? = null

        // When
        val exception = assertFailsWith<BusinessException> {
            value.ifNullThrow()
        }

        // Then
        assertEquals(CommonError.NULL_CASTING_ERROR, exception.error)
    }

    @Test
    fun `예외가 GlobalExceptionHandler의 처리 범위를 벗어나는 경우 오류를 직접 만들어 응답`() {
        // Given
        val response = MockHttpServletResponse()

        // When
        response.sendErrorResponse(JsonMapper.builder().build(), AuthError.ACCESS_DENIED)

        // Then
        assertEquals(200, response.status)
        assertEquals("application/json;charset=UTF-8", response.contentType)
        assertEquals("UTF-8", response.characterEncoding)
        assertTrue(response.contentAsString.contains("\"success\":false"))
        assertTrue(response.contentAsString.contains(AuthError.ACCESS_DENIED.statusCode))
        assertTrue(response.contentAsString.contains(AuthError.ACCESS_DENIED.message))
    }
}
