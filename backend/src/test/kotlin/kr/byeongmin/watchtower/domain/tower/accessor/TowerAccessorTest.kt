package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition
import kr.byeongmin.watchtower.domain.tower.entity.TowerNotificationChannel
import kr.byeongmin.watchtower.domain.tower.repository.*
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TowerAccessorTest {
    private val towerRepository = mock<TowerRepository>()
    private val towerQuerydslRepository = mock<TowerQuerydslRepository>()
    private val towerConditionRepository = mock<TowerConditionRepository>()
    private val towerNotificationChannelRepository = mock<TowerNotificationChannelRepository>()
    private val towerSnapshotQuerydslRepository = mock<TowerSnapshotQuerydslRepository>()
    private val towerConditionQuerydslRepository = mock<TowerConditionQuerydslRepository>()
    private val towerNotificationChannelQuerydslRepository = mock<TowerNotificationChannelQuerydslRepository>()
    private val accessor = TowerAccessor(
        towerRepository = towerRepository,
        towerQuerydslRepository = towerQuerydslRepository,
        towerConditionRepository = towerConditionRepository,
        towerNotificationChannelRepository = towerNotificationChannelRepository,
        towerSnapshotQuerydslRepository = towerSnapshotQuerydslRepository,
        towerConditionQuerydslRepository = towerConditionQuerydslRepository,
        towerNotificationChannelQuerydslRepository = towerNotificationChannelQuerydslRepository
    )

    @Test
    fun `타워를 먼저 저장한 뒤 연결한 알림 채널과 감지 조건을 저장함`() {
        /** given */
        val tower = mock<Tower>()
        val conditions = listOf(mock<TowerCondition>())
        val channels = listOf(mock<TowerNotificationChannel>())

        /** when */
        accessor.saveTower(tower, conditions, channels)

        /** then */
        inOrder(towerRepository, towerNotificationChannelRepository, towerConditionRepository) {
            verify(towerRepository).save(tower)
            verify(towerNotificationChannelRepository).saveAll(channels)
            verify(towerConditionRepository).saveAll(conditions)
        }
    }

    @Test
    fun `알림 채널을 선택하지 않은 타워는 채널 연결을 저장하지 않음`() {
        /** given */
        val tower = mock<Tower>()
        val conditions = listOf(mock<TowerCondition>())

        /** when */
        accessor.saveTower(tower, conditions, emptyList())

        /** then */
        verifyNoInteractions(towerNotificationChannelRepository)
        inOrder(towerRepository, towerConditionRepository) {
            verify(towerRepository).save(tower)
            verify(towerConditionRepository).saveAll(conditions)
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
        accessor.deleteAllRelatedByMemberId(21L, deletedAt)

        /** then */
        inOrder(
            towerSnapshotQuerydslRepository,
            towerConditionQuerydslRepository,
            towerNotificationChannelQuerydslRepository,
            towerQuerydslRepository
        ) {
            verify(towerSnapshotQuerydslRepository).deleteByMemberId(21L, deletedAt)
            verify(towerConditionQuerydslRepository).deleteByMemberId(21L, deletedAt)
            verify(towerNotificationChannelQuerydslRepository).deleteByMemberId(21L, deletedAt)
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
            accessor.deleteAllRelatedByMemberId(21L, deletedAt)
        }

        /** then */
        verifyNoInteractions(
            towerConditionQuerydslRepository,
            towerNotificationChannelQuerydslRepository,
            towerQuerydslRepository
        )
    }
}
