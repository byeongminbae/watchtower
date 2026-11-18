package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CssSelectorTest {
    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `공백뿐인 선택자는 만들 수 없음`(value: String) {
        /** when */
        val error = assertFailsWith<BusinessException> {
            CssSelector(value)
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, error.error)
    }

    @Test
    fun `최대 길이까지는 만들 수 있고 넘으면 만들 수 없음`() {
        /** when */
        val maxLengthSelector = CssSelector("a".repeat(TowerPolicy.MAX_DETECTION_AREA_SELECTOR_LENGTH))
        val error = assertFailsWith<BusinessException> {
            CssSelector("a".repeat(TowerPolicy.MAX_DETECTION_AREA_SELECTOR_LENGTH + 1))
        }

        /** then */
        assertEquals(TowerPolicy.MAX_DETECTION_AREA_SELECTOR_LENGTH, maxLengthSelector.value.length)
        assertEquals(CommonError.INVALID_INPUT_VALUE, error.error)
    }
}
