package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KeywordTest {
    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `공백뿐인 키워드는 만들 수 없음`(value: String) {
        /** when */
        val error = assertFailsWith<BusinessException> {
            Keyword(value)
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, error.error)
    }
}
