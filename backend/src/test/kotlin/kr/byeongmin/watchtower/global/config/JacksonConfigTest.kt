package kr.byeongmin.watchtower.global.config

import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals

class JacksonConfigTest {
    private val objectMapper = JsonMapper.builder()
        .also { builder -> JacksonConfig().utcLocalDateTimeCustomizer().customize(builder) }
        .build()

    @Test
    fun `서버 시각을 응답으로 보내면 UTC 표시를 붙여 직렬화함`() {
        /** given */
        val value = LocalDateTime.of(2026, 10, 27, 3, 0, 5, 123_000_000)

        /** when */
        val result = objectMapper.writeValueAsString(value)

        /** then */
        assertEquals("\"2026-10-27T03:00:05.123Z\"", result)
    }
}
