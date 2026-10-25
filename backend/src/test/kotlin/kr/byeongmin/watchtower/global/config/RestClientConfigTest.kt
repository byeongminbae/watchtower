package kr.byeongmin.watchtower.global.config

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test

class RestClientConfigTest {
    @Test
    fun `외부 서비스가 실패하면 외부 API 오류로 처리함`() {
        /** given */
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/always-fail") { exchange ->
            val body = "외부 서비스 실패".toByteArray()
            exchange.sendResponseHeaders(500, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()

        try {
            /** when */
            val exception = assertFailsWith<BusinessException> {
                RestClientConfig().restClient()
                    .get()
                    .uri("http://127.0.0.1:${server.address.port}/always-fail")
                    .retrieve()
                    .toBodilessEntity()
            }

            /** then */
            assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
        } finally {
            server.stop(0)
        }
    }
}
