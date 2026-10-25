package kr.byeongmin.watchtower.domain.tower.service

import java.time.LocalDateTime
import kotlin.test.assertEquals
import kr.byeongmin.watchtower.domain.member.policy.MemberPolicy
import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.global.config.TimeProvider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TowerMaintenanceServiceTest {

    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val towerAccessor = mock<TowerAccessor>()
    private val timeProvider = mock<TimeProvider>()

    private val towerMaintenanceService = TowerMaintenanceService(
        towerSnapshotAccessor = towerSnapshotAccessor,
        towerAccessor = towerAccessor,
        timeProvider = timeProvider
    )

    private val now = LocalDateTime.of(2026, 9, 20, 12, 0)

    @Test
    fun `스냅샷 정리는 설정된 보관 기간이 지난 기록을 대상으로 함`() {
        /** given */
        val retentionCutoff = now.minus(TowerPolicy.SNAPSHOT_RETENTION_PERIOD)
        whenever(timeProvider.now()).thenReturn(now)
        whenever(towerSnapshotAccessor.deleteByCreatedAtBefore(retentionCutoff, now))
            .thenReturn(2L)

        /** when */
        val deletedCount = towerMaintenanceService.deleteExpiredSnapshots()

        /** then */
        assertEquals(2L, deletedCount)
    }

    @Test
    fun `정리할 스냅샷이 없으면 삭제 건수는 0임`() {
        /** given */
        val retentionCutoff = now.minus(TowerPolicy.SNAPSHOT_RETENTION_PERIOD)
        whenever(timeProvider.now()).thenReturn(now)
        whenever(towerSnapshotAccessor.deleteByCreatedAtBefore(retentionCutoff, now))
            .thenReturn(0L)

        /** when */
        val deletedCount = towerMaintenanceService.deleteExpiredSnapshots()

        /** then */
        assertEquals(0L, deletedCount)
        verify(towerSnapshotAccessor).deleteByCreatedAtBefore(retentionCutoff, now)
    }

    @Test
    fun `휴면 기간이 지난 회원의 타워를 일시정지할 수 있음`() {
        /** given */
        val inactivityCutoff = now.minus(MemberPolicy.INACTIVITY_PERIOD)
        whenever(timeProvider.now()).thenReturn(now)
        whenever(towerAccessor.pauseByMemberLastSignInAtBefore(inactivityCutoff, now))
            .thenReturn(2L)

        /** when */
        val pausedCount = towerMaintenanceService.pauseTowersOfInactiveMembers()

        /** then */
        assertEquals(2L, pausedCount)
    }

    @Test
    fun `휴면으로 정지할 타워가 없으면 정지 건수는 0임`() {
        /** given */
        val inactivityCutoff = now.minus(MemberPolicy.INACTIVITY_PERIOD)
        whenever(timeProvider.now()).thenReturn(now)
        whenever(towerAccessor.pauseByMemberLastSignInAtBefore(inactivityCutoff, now))
            .thenReturn(0L)

        /** when */
        val pausedCount = towerMaintenanceService.pauseTowersOfInactiveMembers()

        /** then */
        assertEquals(0L, pausedCount)
        verify(towerAccessor).pauseByMemberLastSignInAtBefore(inactivityCutoff, now)
    }
}
