package kr.byeongmin.watchtower.global.config

import io.swagger.v3.oas.models.security.SecurityScheme
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OpenApiConfigTests {
    @Test
    fun `open api exposes bearer jwt authorization scheme`() {
        val openApi = OpenApiConfig().openApi()

        val securityScheme = assertNotNull(openApi.components.securitySchemes["bearerAuth"])
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.type)
        assertEquals("bearer", securityScheme.scheme)
        assertEquals("JWT", securityScheme.bearerFormat)
        assertEquals(listOf("bearerAuth"), openApi.security.single().keys.toList())
    }
}
