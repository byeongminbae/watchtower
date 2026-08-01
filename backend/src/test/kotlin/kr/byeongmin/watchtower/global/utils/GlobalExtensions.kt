package kr.byeongmin.watchtower.global.utils

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

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
}
