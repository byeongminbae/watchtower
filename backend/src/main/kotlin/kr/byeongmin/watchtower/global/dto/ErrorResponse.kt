package kr.byeongmin.watchtower.global.dto

import kr.byeongmin.watchtower.global.exception.BusinessException
import java.time.LocalDateTime

/**
 *  [BusinessException] 발생 시 이 클래스에 [BusinessException] 에 포함된 필드를 포함해 에러 응답에 사용되는 클래스
 * 서블릿에 도달하기 전 코드를 작성하지 않는 이상 개발자가 직접 이 응답을 쓸일은 없음
 * */
class ErrorResponse(businessException: BusinessException) : Response {
    override val isSuccess: Boolean = false
    val statusCode: String = businessException.error.statusCode
    val message: String = businessException.error.message
    override val timestamp: LocalDateTime = businessException.timestamp
}