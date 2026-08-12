package kr.byeongmin.watchtower.global.exception

import kr.byeongmin.watchtower.global.error.CommonError
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GlobalExceptionHandlerTest {
    @Test
    fun `비즈니스 예외가 주어진 상황에서 전역 예외 핸들러가 처리하면 예외 정보를 담은 오류 응답을 반환한다`() {
        // Given
        val timestamp = LocalDateTime.of(2026, 7, 31, 13, 0)
        val exception = BusinessException(CommonError.INVALID_INPUT_VALUE, timestamp)
        val request = MockHttpServletRequest("GET", "/api/v1/example").apply {
            addParameter("검색어", "첫째", "둘째")
        }

        // When
        val response = GlobalExceptionHandler().handleBusinessException(exception, request)

        // Then
        assertFalse(response.success)
        assertEquals(CommonError.INVALID_INPUT_VALUE.statusCode, response.statusCode)
        assertEquals(CommonError.INVALID_INPUT_VALUE.message, response.message)
        assertEquals(timestamp, response.timestamp)
    }
}
