package kr.byeongmin.watchtower.global.response

import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

class SuccessResponse<T>(val data: T) : Response {
    override val timestamp: LocalDateTime = TimeUtil.currentKSTTime()
}