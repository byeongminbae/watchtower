package kr.byeongmin.watchtower.global.response

import kr.byeongmin.watchtower.global.exception.BusinessException
import java.time.LocalDateTime

class ErrorResponse(businessException: BusinessException) : Response {
    val statusCode: String = businessException.error.statusCode
    val message: String = businessException.error.message
    override val timestamp: LocalDateTime = businessException.timestamp
}