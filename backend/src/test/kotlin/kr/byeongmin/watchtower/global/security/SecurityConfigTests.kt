package kr.byeongmin.watchtower.global.security

import kr.byeongmin.watchtower.global.error.AuthError
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SecurityConfigTests {
    private val objectMapper = ObjectMapper()
    private val securityConfig = SecurityConfig(objectMapper)

    @Test
    fun `cors allows credentialed frontend origins`() {
        val source = securityConfig.corsConfigurationSource()
        val configuration = assertNotNull(source.getCorsConfiguration(MockHttpServletRequest()))

        assertEquals(
            listOf(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "https://front-dev.watchtower.boo",
                "https://watchtower.boo",
            ),
            configuration.allowedOrigins,
        )
        assertEquals(true, configuration.allowCredentials)
    }

    @Test
    fun `security error is written as error response json`() {
        val response = MockHttpServletResponse()

        securityConfig.writeErrorResponse(
            response,
            AuthError.UNAUTHORIZED,
        )

        val body = objectMapper.readTree(response.contentAsString)
        assertEquals(200, response.status)
        assertEquals("${MediaType.APPLICATION_JSON_VALUE};charset=UTF-8", response.contentType)
        assertEquals(StandardCharsets.UTF_8.name(), response.characterEncoding)
        assertEquals(
            "인증이 필요합니다.",
            objectMapper
                .readTree(response.contentAsByteArray.toString(StandardCharsets.UTF_8))["message"]
                .stringValue(),
        )
        assertEquals(false, body["success"].booleanValue())
        assertEquals("AUT_001", body["statusCode"].stringValue())
        assertEquals("인증이 필요합니다.", body["message"].stringValue())
        assertNotNull(body["timestamp"].stringValue())
    }
}
