package kr.byeongmin.watchtower.global.config

import com.sun.net.httpserver.HttpServer
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RestClientConfigTest {
    @Test
    fun `외부 API에서 오류가 발생한 경우`() {
        // Given
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/always-fail") { exchange ->
            val body = "외부 서비스 실패".toByteArray()
            exchange.sendResponseHeaders(500, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        // When
        try {
            val exception = assertFailsWith<BusinessException> {
                RestClientConfig().restClient()
                    .get()
                    .uri("http://127.0.0.1:${server.address.port}/always-fail")
                    .retrieve()
                    .toBodilessEntity()
            }

            // Then
            assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
        } finally {
            server.stop(0)
        }
    }
}
