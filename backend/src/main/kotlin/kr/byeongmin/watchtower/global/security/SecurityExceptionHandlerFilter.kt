package kr.byeongmin.watchtower.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.response.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

class SecurityExceptionHandlerFilter(
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter(), AuthenticationEntryPoint, AccessDeniedHandler {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            filterChain.doFilter(request, response)
        } catch (exception: BusinessException) {
            writeErrorResponse(response, exception)
        }
    }

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        writeErrorResponse(response, BusinessException(AuthError.UNAUTHORIZED))
    }

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: org.springframework.security.access.AccessDeniedException,
    ) {
        writeErrorResponse(response, BusinessException(AuthError.ACCESS_DENIED))
    }

    private fun writeErrorResponse(
        response: HttpServletResponse,
        exception: BusinessException,
    ) {
        with(response) {
            status = HttpStatus.OK.value()
            contentType = MediaType.APPLICATION_JSON_VALUE
            characterEncoding = StandardCharsets.UTF_8.name()
            objectMapper.writeValue(writer, ErrorResponse(exception))
        }
    }
}
