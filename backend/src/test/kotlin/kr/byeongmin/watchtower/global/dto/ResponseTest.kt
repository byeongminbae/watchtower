package kr.byeongmin.watchtower.global.dto

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ResponseTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `성공 응답은 전달한 데이터를 그대로 담음`() {
        /** given */
        val data = listOf("첫 번째", "두 번째")

        /** when */
        val response = SuccessDataResponse(data)

        /** then */
        assertTrue(response.isSuccess)
        assertSame(data, response.data)
    }

    @Test
    fun `반환할 데이터가 없어도 요청 성공을 표현할 수 있음`() {
        /** when */
        val response = SuccessResponse()

        /** then */
        assertTrue(response.isSuccess)
    }

    @Test
    fun `오류 응답에서 실패 원인과 발생 시각을 확인할 수 있음`() {
        /** given */
        val timestamp = LocalDateTime.of(2026, 7, 31, 12, 0)

        /** when */
        val response = ErrorResponse(BusinessException(CommonError.RESOURCE_NOT_FOUND, timestamp))

        /** then */
        assertFalse(response.isSuccess)
        assertEquals(CommonError.RESOURCE_NOT_FOUND.statusCode, response.statusCode)
        assertEquals(CommonError.RESOURCE_NOT_FOUND.message, response.message)
        assertEquals(timestamp, response.timestamp)
    }

    @Test
    fun `프론트는 isSuccess 필드로 요청 성공 여부를 구분할 수 있음`() {
        /** given */
        val responses = listOf(
            SuccessResponse(),
            SuccessDataResponse("데이터"),
            ErrorResponse(BusinessException(CommonError.RESOURCE_NOT_FOUND))
        )

        responses.forEach { response ->
            /** when */
            val body = objectMapper.readTree(objectMapper.writeValueAsBytes(response))

            /** then */
            assertEquals(response.isSuccess, body["isSuccess"]?.booleanValue())
            assertTrue(body.has("timestamp"))
        }
    }
}
