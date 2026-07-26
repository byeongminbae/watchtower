package kr.byeongmin.watchtower.global.security

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SecurityConfigTests {
    @Test
    fun `cors allows credentialed frontend origins`() {
        val source = SecurityConfig().corsConfigurationSource()
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
}
