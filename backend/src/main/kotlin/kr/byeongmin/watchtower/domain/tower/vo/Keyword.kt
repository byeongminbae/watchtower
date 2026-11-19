package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException

data class Keyword(val value: String) {
    init {
        if (value.isBlank()) {
            throw BusinessException(CommonError.INVALID_INPUT_VALUE)
        }
    }
}
