package kr.byeongmin.watchtower.global.config

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals

class JpaAuditingConfigTest {
    @Test
    fun `감사 시각은 고정되지 않고 매번 현재 시각을 사용함`() {
        /** given */
        val timeProvider = mock<TimeProvider>()
        val first = LocalDateTime.of(2026, 10, 20, 12, 0)
        val second = first.plusHours(1)
        whenever(timeProvider.now()).thenReturn(first, second)
        val provider = JpaAuditingConfig().auditingDateTimeProvider(timeProvider)

        /** when */
        val firstResult = provider.now.orElseThrow()
        val secondResult = provider.now.orElseThrow()

        /** then */
        assertEquals(first, firstResult)
        assertEquals(second, secondResult)
    }
}
