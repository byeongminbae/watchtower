package kr.byeongmin.watchtower.global.config

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class TimeProviderTest {
    private val timeProvider = TimeProvider()

    @Test
    fun `현재 시각은 UTC 기준으로 제공됨`() {
        /** given */
        val before = LocalDateTime.now(ZoneId.of("UTC")).minusSeconds(1)

        /** when */
        val result = timeProvider.now()

        /** then */
        val after = LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(1)

        assertTrue(result.isAfter(before))
        assertTrue(result.isBefore(after))
    }
}
