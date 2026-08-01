package kr.byeongmin.watchtower.global.utils

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertTrue

class TimeUtilTest {
    @Test
    fun `엔티티 시각은 협정 세계시를 사용`() {
        // When
        val before = LocalDateTime.now(ZoneId.of("UTC")).minusSeconds(1)
        val result = TimeUtil.entityTime()
        val after = LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(1)

        // Then
        assertTrue(result.isAfter(before))
        assertTrue(result.isBefore(after))
    }

    @Test
    fun `디버깅 시각은 서울 표준시를 사용`() {
        // When
        val before = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusSeconds(1)
        val result = TimeUtil.debuggingTime()
        val after = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusSeconds(1)

        // Then
        assertTrue(result.isAfter(before))
        assertTrue(result.isBefore(after))
    }
}
