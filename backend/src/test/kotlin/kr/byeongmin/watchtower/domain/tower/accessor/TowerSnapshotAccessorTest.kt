package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotQuerydslRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TowerSnapshotAccessorTest {
    private val towerSnapshotRepository = mock<TowerSnapshotRepository>()
    private val towerSnapshotQuerydslRepository = mock<TowerSnapshotQuerydslRepository>()
    private val towerSnapshotAccessor = TowerSnapshotAccessor(towerSnapshotRepository, towerSnapshotQuerydslRepository)

    @Test
    fun `조회할 타워가 없으면 쿼리 없이 빈 요약 목록을 돌려줌`() {
        /** when */
        val summaries = towerSnapshotAccessor.findLatestChangeAiSummaries(emptyList())

        /** then */
        assertTrue(summaries.isEmpty())
        verifyNoInteractions(towerSnapshotQuerydslRepository)
    }

    @Test
    fun `변경이 감지된 적 없는 타워의 마지막 변경 AI 요약은 없음`() {
        /** given */
        whenever(towerSnapshotQuerydslRepository.findLatestChangeAiSummariesByTowerIds(listOf(7L)))
            .thenReturn(emptyList())

        /** when */
        val aiSummary = towerSnapshotAccessor.findLatestChangeAiSummary(7L)

        /** then */
        assertNull(aiSummary)
    }

    @Test
    fun `가장 최근 스냅샷을 타워 ID 로 찾을 수 있게 돌려줌`() {
        /** given */
        val snapshotOfTower7 = snapshotOf(towerId = 7L)
        val snapshotOfTower8 = snapshotOf(towerId = 8L)
        whenever(towerSnapshotQuerydslRepository.findLatestByTowerIds(listOf(7L, 8L, 9L)))
            .thenReturn(listOf(snapshotOfTower7, snapshotOfTower8))

        /** when */
        val latestSnapshots = towerSnapshotAccessor.findLatestSnapshots(listOf(7L, 8L, 9L))

        /** then */
        assertEquals(mapOf(7L to snapshotOfTower7, 8L to snapshotOfTower8), latestSnapshots)
    }

    @Test
    fun `가장 최근 스냅샷을 찾을 타워가 없으면 쿼리 없이 빈 결과를 돌려줌`() {
        /** when */
        val latestSnapshots = towerSnapshotAccessor.findLatestSnapshots(emptyList())

        /** then */
        assertTrue(latestSnapshots.isEmpty())
        verifyNoInteractions(towerSnapshotQuerydslRepository)
    }

    @Test
    fun `다른 타워의 스냅샷이거나 삭제된 스냅샷은 찾을 수 없음`() {
        /** given */
        val tower = mock<Tower>()
        whenever(towerSnapshotRepository.findByIdAndTowerAndDeletedAtIsNull(11L, tower)).thenReturn(null)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerSnapshotAccessor.getSnapshot(tower, 11L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    private fun snapshotOf(towerId: Long): TowerSnapshot {
        val tower = mock<Tower> {
            on { id } doReturn towerId
        }
        return mock<TowerSnapshot> {
            on { this.tower } doReturn tower
        }
    }
}
