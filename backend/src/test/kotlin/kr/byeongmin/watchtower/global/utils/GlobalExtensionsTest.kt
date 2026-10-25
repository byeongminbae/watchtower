package kr.byeongmin.watchtower.global.utils

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GlobalExtensionsTest {
    @Test
    fun `필수값이 있으면 원본 객체를 그대로 사용할 수 있음`() {
        /** given */
        val value = mutableListOf("값")

        /** when */
        val result = value.ifNullThrow()

        /** then */
        assertSame(value, result)
    }

    @Test
    fun `필수값이 없으면 널 캐스팅 비즈니스 예외가 발생함`() {
        /** given */
        val value: String? = null

        /** when */
        val exception = assertFailsWith<BusinessException> {
            value.ifNullThrow()
        }

        /** then */
        assertEquals(CommonError.NULL_CASTING_ERROR, exception.error)
    }
}
