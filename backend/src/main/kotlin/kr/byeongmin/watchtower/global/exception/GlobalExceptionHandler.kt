package kr.byeongmin.watchtower.global.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import kr.byeongmin.watchtower.global.dto.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 서블릿 이후에 발생한 예외 처리기
 *
 * 프로젝트 컨벤션상 응답 코드는 무조건 200 이고 봉투패턴을 사용해 응답함
 * */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        businessException: BusinessException,
        request: HttpServletRequest
    ): ErrorResponse {
        logger.debug(businessException) {
            getHttpHeader(request)
        }
        return ErrorResponse(businessException)
    }

    private fun getHttpHeader(
        request: HttpServletRequest
    ): String {
        return StringBuilder().append("\n")
            .append("requestMethod: ${request.method}").append("\n")
            .append("requestURL: ${request.requestURL}").append("\n")
            .append("parameterMap: ${getParameters(request)}")
            .toString()
    }

    private fun getParameters(request: HttpServletRequest): String {
        return request.parameterMap.entries.joinToString(", ") { (key, values) ->
            "$key=${values.joinToString(",")}"
        }
    }
}
