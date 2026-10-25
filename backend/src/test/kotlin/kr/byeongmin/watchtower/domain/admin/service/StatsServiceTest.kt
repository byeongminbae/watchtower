package kr.byeongmin.watchtower.domain.admin.service

import java.time.LocalDateTime
import kotlin.test.assertEquals
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.global.config.TimeProvider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class StatsServiceTest {
    private val towerAccessor = mock<TowerAccessor>()
    private val memberAccessor = mock<MemberAccessor>()
    private val timeProvider = mock<TimeProvider>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)

    private val statsService = StatsService(
        towerAccessor = towerAccessor,
        memberAccessor = memberAccessor,
        timeProvider = timeProvider
    )

    @Test
    fun `관리자는 전체 타워 수와 상태별 타워 수를 조회할 수 있음`() {
        /** given */
        whenever(towerAccessor.countByStatus(TowerStatus.RUNNING)).thenReturn(5L)
        whenever(towerAccessor.countByStatus(TowerStatus.PAUSED)).thenReturn(3L)
        whenever(towerAccessor.countByStatus(TowerStatus.ILLEGAL_SUSPENDED)).thenReturn(1L)

        /** when */
        val response = statsService.getTowerStats()

        /** then */
        assertEquals(9L, response.data.totalCount)
        assertEquals(5L, response.data.runningCount)
        assertEquals(3L, response.data.pausedCount)
        assertEquals(1L, response.data.illegalSuspendedCount)
    }

    @Test
    fun `관리자는 전체 회원 수와 상태별 회원 수를 조회할 수 있음`() {
        /** given */
        whenever(memberAccessor.count()).thenReturn(10L)
        whenever(memberAccessor.countActive()).thenReturn(8L)
        whenever(memberAccessor.countBanned()).thenReturn(2L)
        whenever(memberAccessor.countDeleted()).thenReturn(4L)

        /** when */
        val response = statsService.getMemberStats()

        /** then */
        assertEquals(10L, response.data.totalCount)
        assertEquals(8L, response.data.activeCount)
        assertEquals(2L, response.data.bannedCount)
        assertEquals(4L, response.data.deletedCount)
    }

    @Test
    fun `관리자는 최근 7일과 30일의 가입 회원 수와 로그인 회원 수를 조회할 수 있음`() {
        /** given */
        whenever(timeProvider.now()).thenReturn(now)
        whenever(memberAccessor.countByCreatedAtAfter(now.minusDays(7))).thenReturn(3L)
        whenever(memberAccessor.countByCreatedAtAfter(now.minusDays(30))).thenReturn(12L)
        whenever(memberAccessor.countByLastSignInAtAfter(now.minusDays(7))).thenReturn(7L)
        whenever(memberAccessor.countByLastSignInAtAfter(now.minusDays(30))).thenReturn(14L)

        /** when */
        val response = statsService.getAuthStats()

        /** then */
        assertEquals(3L, response.data.signUps.last7Days)
        assertEquals(12L, response.data.signUps.last30Days)
        assertEquals(7L, response.data.signIns.last7Days)
        assertEquals(14L, response.data.signIns.last30Days)
    }
}
