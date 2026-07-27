package kr.byeongmin.watchtower.global.config

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestClient

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
