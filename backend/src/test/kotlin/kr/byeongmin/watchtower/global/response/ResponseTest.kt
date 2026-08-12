package kr.byeongmin.watchtower.global.response

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ResponseTest {
    @Test
    fun `데이터가 있는 상황에서 성공 응답을 생성하면 성공 상태와 데이터를 반환한다`() {
        // Given
        val data = mutableListOf("첫 번째", "두 번째")

        // When
        val response = SuccessDataResponse(data)

        // Then
        assertTrue(response.success)
        assertSame(data, response.data)
    }

    @Test
    fun `반환할 데이터가 없는 상황에서 성공 응답을 생성하면 성공 상태를 반환한다`() {
        // When
        val response = SuccessResponse()

        // Then
        assertTrue(response.success)
    }

    @Test
    fun `비즈니스 예외가 있는 상황에서 오류 응답을 생성하면 실패 상태와 예외 정보를 반환한다`() {
        // Given
        val timestamp = LocalDateTime.of(2026, 7, 31, 12, 0)
        val exception = BusinessException(CommonError.RESOURCE_NOT_FOUND, timestamp)

        // When
        val response = ErrorResponse(exception)

        // Then
        assertFalse(response.success)
        assertEquals(CommonError.RESOURCE_NOT_FOUND.statusCode, response.statusCode)
        assertEquals(CommonError.RESOURCE_NOT_FOUND.message, response.message)
        assertEquals(timestamp, response.timestamp)
    }
}
