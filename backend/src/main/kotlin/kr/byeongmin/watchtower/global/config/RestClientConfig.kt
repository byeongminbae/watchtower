package kr.byeongmin.watchtower.global.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClient

/** 외부 HTTP 오류를 공통 비즈니스 익셉션으로 변환하는 설정 클래스 */
@Configuration
class RestClientConfig {
    private val logger = KotlinLogging.logger {}

    private fun getLogMessage(
        request: HttpRequest,
        response: ClientHttpResponse
    ): String {
        return StringBuilder().append("\n")
            .append("==================== 외부 API 요청 ====================").append("\n")
            .append("requestMethod: ${request.method}").append("\n")
            .append("requestUri: ${request.uri}").append("\n")
            .append("requestHeaders: ${request.headers}").append("\n")
            .append("responseHeaders: ${response.headers}").append("\n")
            .append("responseBody: ${response.body.bufferedReader().readText()}").append("\n")
            .append("==================== 외부 API 요청 ====================").append("\n")
            .toString()
    }

    /** HTTP 4xx·5xx 응답을 EXTERNAL_API_ERROR로 변환한다. 연결 실패와 빈 본문은 여기서 변환하지 않는다. */
    @Bean
    fun restClient(): RestClient {
        return RestClient.builder()
            .defaultStatusHandler(
                { status -> status.isError },
                { request, response ->
                    val responseBody = getLogMessage(request, response)
                    logger.debug { responseBody }
                    throw BusinessException(CommonError.EXTERNAL_API_ERROR)
                }
            )
            .build()
    }
}
