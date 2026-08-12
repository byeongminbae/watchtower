package kr.byeongmin.watchtower.global.exception

import kr.byeongmin.watchtower.global.error.Error
import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

class BusinessException(
    val error: Error,
    val timestamp: LocalDateTime = TimeUtil.debuggingTime()
) : RuntimeException() {
    override fun toString(): String {
        return "BusinessException(error=$error, timestamp=$timestamp)"
    }
}