package kr.byeongmin.watchtower.global.utils

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException

fun <T> T?.ifNullThrow(): T {
    if (this == null) throw BusinessException(CommonError.NULL_CASTING_ERROR)
    return this
}
