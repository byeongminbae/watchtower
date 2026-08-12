package kr.byeongmin.watchtower.global.utils

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.assertTrue

class TimeUtilTest {
    @Test
    fun `엔티티 시각을 사용하는 상황에서 현재 시각을 조회하면 협정 세계시를 반환한다`() {
        // When
        val before = LocalDateTime.now(ZoneId.of("UTC")).minusSeconds(1)
        val result = TimeUtil.entityTime()
        val after = LocalDateTime.now(ZoneId.of("UTC")).plusSeconds(1)

        // Then
        assertTrue(result.isAfter(before))
        assertTrue(result.isBefore(after))
    }

    @Test
    fun `디버깅 시각을 사용하는 상황에서 현재 시각을 조회하면 서울 표준시를 반환한다`() {
        // When
        val before = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusSeconds(1)
        val result = TimeUtil.debuggingTime()
        val after = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusSeconds(1)

        // Then
        assertTrue(result.isAfter(before))
        assertTrue(result.isBefore(after))
    }
}
