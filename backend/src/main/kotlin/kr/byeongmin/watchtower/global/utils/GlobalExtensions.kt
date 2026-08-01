package kr.byeongmin.watchtower.global.utils

import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.error.Error
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

fun <T> T?.ifNullThrow(): T {
    if (this == null) throw BusinessException(CommonError.NULL_CASTING_ERROR)
    return this
}

fun HttpServletResponse.sendErrorResponse(objectMapper: ObjectMapper, error: Error) {
    with(this) {
        status = HttpStatus.OK.value()
        contentType = MediaType.APPLICATION_JSON_VALUE
        characterEncoding = StandardCharsets.UTF_8.name()
        objectMapper.writeValue(writer, ErrorResponse(BusinessException(error)))
    }
}