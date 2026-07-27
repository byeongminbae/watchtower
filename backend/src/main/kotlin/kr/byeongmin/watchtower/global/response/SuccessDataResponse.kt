package kr.byeongmin.watchtower.global.response

import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

class SuccessDataResponse<T>(val data: T) : Response {
    override val success: Boolean = true
    override val timestamp: LocalDateTime = TimeUtil.debuggingTime()
}