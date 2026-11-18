package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DetectionConditionResponseDtoTest {
    @Test
    fun `지원하지 않는 감지 조건의 응답을 만들면 비즈니스 오류로 처리함`() {
        /** given */
        val unsupportedCondition = object : DetectionCondition(mock<Tower>()) {
            override fun isConditionSatisfied(previous: DetectionContent, current: DetectionContent): Boolean {
                return false
            }
        }

        /** when */
        val exception = assertFailsWith<BusinessException> {
            DetectionConditionResponseDto.from(unsupportedCondition)
        }

        /** then */
        assertEquals(TowerError.UNSUPPORTED_CONDITION, exception.error)
    }
}
