package kr.byeongmin.watchtower.global.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import kr.byeongmin.watchtower.global.response.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(businessException: BusinessException, request: HttpServletRequest): ErrorResponse {
        logger.debug(businessException) {
            "\n${request.requestURL}"
        }

        return ErrorResponse(businessException)
    }
}
