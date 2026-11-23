package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.repository.*
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class TowerAccessorTest {
    private val towerRepository = mock<TowerRepository>()
    private val towerQuerydslRepository = mock<TowerQuerydslRepository>()
    private val detectionConditionRepository = mock<DetectionConditionRepository>()
    private val towerSnapshotQuerydslRepository = mock<TowerSnapshotQuerydslRepository>()
    private val detectionConditionQuerydslRepository = mock<DetectionConditionQuerydslRepository>()
    private val accessor = TowerAccessor(
        towerRepository = towerRepository,
        towerQuerydslRepository = towerQuerydslRepository,
        detectionConditionRepository = detectionConditionRepository,
        towerSnapshotQuerydslRepository = towerSnapshotQuerydslRepository,
        detectionConditionQuerydslRepository = detectionConditionQuerydslRepository
    )

    @Test
    fun `타워를 먼저 저장한 뒤 감지 조건을 저장하고 저장된 타워를 돌려줌`() {
        /** given */
        val tower = mock<Tower>()
        val savedTower = mock<Tower>()
        val conditions = listOf(mock<DetectionCondition>())
        whenever(towerRepository.save(tower)).thenReturn(savedTower)

        /** when */
        val result = accessor.saveTower(tower, conditions)

        /** then */
        assertSame(savedTower, result)
        inOrder(towerRepository, detectionConditionRepository) {
            verify(towerRepository).save(tower)
            verify(detectionConditionRepository).saveAll(conditions)
        }
    }

    @Test
    fun `소유한 타워를 찾을 수 없으면 조회에 실패함`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            accessor.getOwnedTower(towerId = 7L, memberId = 21L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(towerRepository).findByIdAndCreatedBy_IdAndDeletedAtIsNull(7L, 21L)
    }

    @Test
    fun `잠글 수 있는 소유 타워를 찾을 수 없으면 조회에 실패함`() {
        /** given */
        whenever(towerRepository.findTowerForUpdateByIdAndCreatedBy_IdAndDeletedAtIsNull(7L, 21L))
            .thenReturn(null)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            accessor.getOwnedTowerForUpdate(towerId = 7L, memberId = 21L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(towerRepository).findTowerForUpdateByIdAndCreatedBy_IdAndDeletedAtIsNull(7L, 21L)
    }

    @Test
    fun `회원의 타워를 정리할 때 모두 같은 시각으로 삭제 처리함`() {
        /** given */
        val deletedAt = LocalDateTime.of(2026, 10, 22, 12, 0)

        /** when */
        accessor.deleteAllOwnedTowers(21L, deletedAt)

        /** then */
        inOrder(
            towerSnapshotQuerydslRepository,
            detectionConditionQuerydslRepository,
            towerQuerydslRepository
        ) {
            verify(towerSnapshotQuerydslRepository).deleteByMemberId(21L, deletedAt)
            verify(detectionConditionQuerydslRepository).deleteByMemberId(21L, deletedAt)
            verify(towerQuerydslRepository).deleteByMemberId(21L, deletedAt)
        }
    }

    @Test
    fun `스냅샷을 삭제 처리하지 못하면 나머지 타워 자료도 정리하지 않음`() {
        /** given */
        val deletedAt = LocalDateTime.of(2026, 10, 22, 12, 0)
        whenever(towerSnapshotQuerydslRepository.deleteByMemberId(21L, deletedAt))
            .thenThrow(IllegalStateException("snapshot update failed"))

        /** when */
        assertFailsWith<IllegalStateException> {
            accessor.deleteAllOwnedTowers(21L, deletedAt)
        }

        /** then */
        verifyNoInteractions(
            detectionConditionQuerydslRepository,
            towerQuerydslRepository
        )
    }
}
