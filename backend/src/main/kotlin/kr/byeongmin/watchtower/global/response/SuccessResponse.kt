package kr.byeongmin.watchtower.global.response

import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

class SuccessResponse : Response {
    override val success: Boolean = true
    override val timestamp: LocalDateTime = TimeUtil.debuggingTime()
}
