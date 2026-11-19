package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException

data class CssSelector(val value: String) {
    init {
        if (value.isBlank() || value.length > TowerPolicy.MAX_DETECTION_AREA_SELECTOR_LENGTH) {
            throw BusinessException(CommonError.INVALID_INPUT_VALUE)
        }
    }
}
