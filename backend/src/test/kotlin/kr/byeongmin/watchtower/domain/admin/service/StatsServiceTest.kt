package kr.byeongmin.watchtower.domain.admin.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.config.TimeProvider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.test.assertEquals

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
        whenever(towerAccessor.countTowers(TowerStatus.READY)).thenReturn(5L)
        whenever(towerAccessor.countTowers(TowerStatus.ERROR)).thenReturn(4L)
        whenever(towerAccessor.countTowers(TowerStatus.RUNNING)).thenReturn(2L)
        whenever(towerAccessor.countTowers(TowerStatus.PAUSED)).thenReturn(3L)
        whenever(towerAccessor.countTowers(TowerStatus.SUSPENDED)).thenReturn(1L)

        /** when */
        val response = statsService.getTowerStats()

        /** then */
        assertEquals(15L, response.data.totalCount)
        assertEquals(5L, response.data.readyCount)
        assertEquals(4L, response.data.errorCount)
        assertEquals(2L, response.data.runningCount)
        assertEquals(3L, response.data.pausedCount)
        assertEquals(1L, response.data.suspendedCount)
    }

    @Test
    fun `관리자는 전체 회원 수와 상태별 회원 수를 조회할 수 있음`() {
        /** given */
        whenever(memberAccessor.countMembers()).thenReturn(10L)
        whenever(memberAccessor.countActiveMembers()).thenReturn(8L)
        whenever(memberAccessor.countBannedMembers()).thenReturn(2L)
        whenever(memberAccessor.countDeletedMembers()).thenReturn(4L)

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
        whenever(memberAccessor.countSignedUpMembers(now.minusDays(7))).thenReturn(3L)
        whenever(memberAccessor.countSignedUpMembers(now.minusDays(30))).thenReturn(12L)
        whenever(memberAccessor.countSignedInMembers(now.minusDays(7))).thenReturn(7L)
        whenever(memberAccessor.countSignedInMembers(now.minusDays(30))).thenReturn(14L)

        /** when */
        val response = statsService.getAuthStats()

        /** then */
        assertEquals(3L, response.data.signUps.last7Days)
        assertEquals(12L, response.data.signUps.last30Days)
        assertEquals(7L, response.data.signIns.last7Days)
        assertEquals(14L, response.data.signIns.last30Days)
    }
}
