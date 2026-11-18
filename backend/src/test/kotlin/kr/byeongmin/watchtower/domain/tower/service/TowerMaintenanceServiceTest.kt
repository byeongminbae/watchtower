package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.policy.MemberPolicy
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.SnapshotStorageClient
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TowerMaintenanceServiceTest {

    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val towerAccessor = mock<TowerAccessor>()
    private val snapshotStorageClient = mock<SnapshotStorageClient>()
    private val transactionManager = mock<PlatformTransactionManager> {
        on { getTransaction(any()) } doReturn mock<TransactionStatus>()
    }
    private val now = LocalDateTime.of(2026, 9, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }

    private val towerMaintenanceService = TowerMaintenanceService(
        towerSnapshotAccessor = towerSnapshotAccessor,
        towerAccessor = towerAccessor,
        snapshotStorageClient = snapshotStorageClient,
        transactionTemplate = TransactionTemplate(transactionManager),
        timeProvider = timeProvider
    )

    private val retentionCutoff = now.minus(TowerPolicy.SNAPSHOT_RETENTION_PERIOD)

    private fun expiredSnapshot(id: Long, objectNames: List<String>): TowerSnapshot {
        return mock {
            on { this.id } doReturn id
            on { getAllObjectNames() } doReturn objectNames
        }
    }

    @Test
    fun `보관 기간이 지난 스냅샷의 버킷 객체를 먼저 지운 뒤 스냅샷을 논리 삭제함`() {
        /** given */
        val snapshot = expiredSnapshot(1L, listOf("towers/7/2026-09-01T00:00:00.000Z.txt", "screenshot.png"))
        whenever(
            towerSnapshotAccessor.findExpiredSnapshotsExceptLatest(
                retentionCutoff,
                TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE
            )
        )
            .thenReturn(listOf(snapshot), emptyList())
        whenever(towerSnapshotAccessor.deleteSnapshots(listOf(1L), now)).thenReturn(1L)

        /** when */
        val deletedCount = towerMaintenanceService.deleteExpiredSnapshots()

        /** then */
        assertEquals(1L, deletedCount)
        inOrder(snapshotStorageClient, towerSnapshotAccessor) {
            verify(snapshotStorageClient).deleteObject("towers/7/2026-09-01T00:00:00.000Z.txt")
            verify(snapshotStorageClient).deleteObject("screenshot.png")
            verify(towerSnapshotAccessor).deleteSnapshots(listOf(1L), now)
        }
    }

    @Test
    fun `정리할 스냅샷이 남지 않을 때까지 묶음 단위로 다시 조회해 지움`() {
        /** given */
        val fullBatch = (1L..TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE).map {
            expiredSnapshot(
                it,
                listOf("text-$it.txt")
            )
        }
        val lastBatch = listOf(expiredSnapshot(1_000L, listOf("text-1000.txt")))
        whenever(
            towerSnapshotAccessor.findExpiredSnapshotsExceptLatest(
                retentionCutoff,
                TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE
            )
        )
            .thenReturn(fullBatch, lastBatch, emptyList())
        whenever(towerSnapshotAccessor.deleteSnapshots(any(), any()))
            .thenReturn(TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE, 1L)

        /** when */
        val deletedCount = towerMaintenanceService.deleteExpiredSnapshots()

        /** then */
        assertEquals(TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE + 1, deletedCount)
        verify(towerSnapshotAccessor, times(3)).findExpiredSnapshotsExceptLatest(
            retentionCutoff,
            TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE
        )
    }

    @Test
    fun `정리할 스냅샷이 없으면 아무것도 지우지 않고 삭제 건수는 0임`() {
        /** given */
        whenever(
            towerSnapshotAccessor.findExpiredSnapshotsExceptLatest(
                retentionCutoff,
                TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE
            )
        )
            .thenReturn(emptyList())

        /** when */
        val deletedCount = towerMaintenanceService.deleteExpiredSnapshots()

        /** then */
        assertEquals(0L, deletedCount)
        verify(snapshotStorageClient, never()).deleteObject(any())
        verify(towerSnapshotAccessor, never()).deleteSnapshots(any(), any())
    }

    @Test
    fun `버킷 객체를 지우지 못하면 스냅샷을 논리 삭제하지 않고 다음 실행에서 다시 지우도록 남김`() {
        /** given */
        val snapshot = expiredSnapshot(1L, listOf("towers/7/2026-09-01T00:00:00.000Z.txt"))
        whenever(
            towerSnapshotAccessor.findExpiredSnapshotsExceptLatest(
                retentionCutoff,
                TowerPolicy.EXPIRED_SNAPSHOT_BATCH_SIZE
            )
        )
            .thenReturn(listOf(snapshot))
        whenever(snapshotStorageClient.deleteObject(any()))
            .thenThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))

        /** when */
        assertFailsWith<BusinessException> { towerMaintenanceService.deleteExpiredSnapshots() }

        /** then */
        verify(towerSnapshotAccessor, never()).deleteSnapshots(any(), any())
    }

    @Test
    fun `휴면 기간이 지난 회원의 타워를 일시정지할 수 있음`() {
        /** given */
        val inactivityCutoff = now.minus(MemberPolicy.INACTIVITY_PERIOD)
        whenever(towerAccessor.pauseTowersOfInactiveMembers(inactivityCutoff, now))
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
        whenever(towerAccessor.pauseTowersOfInactiveMembers(inactivityCutoff, now))
            .thenReturn(0L)

        /** when */
        val pausedCount = towerMaintenanceService.pauseTowersOfInactiveMembers()

        /** then */
        assertEquals(0L, pausedCount)
        verify(towerAccessor).pauseTowersOfInactiveMembers(inactivityCutoff, now)
    }
}
