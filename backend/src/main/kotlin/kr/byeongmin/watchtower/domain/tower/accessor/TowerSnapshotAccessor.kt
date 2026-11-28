package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotQuerydslRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotRepository
import kr.byeongmin.watchtower.domain.tower.vo.LatestChangeAiSummary
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TowerSnapshotAccessor(
    private val towerSnapshotRepository: TowerSnapshotRepository,
    private val towerSnapshotQuerydslRepository: TowerSnapshotQuerydslRepository
) {
    fun save(towerSnapshot: TowerSnapshot): TowerSnapshot {
        return towerSnapshotRepository.save(towerSnapshot)
    }

    fun findTowerSnapshotOfNotification(notificationId: Long): TowerSnapshot? {
        return towerSnapshotQuerydslRepository.findByNotificationId(notificationId)
    }

    fun getSnapshot(tower: Tower, snapshotId: Long): TowerSnapshot {
        return towerSnapshotRepository.findByIdAndTowerAndDeletedAtIsNull(snapshotId, tower)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun findChangedSnapshots(tower: Tower): List<TowerSnapshot> {
        return towerSnapshotQuerydslRepository.findChangedSnapshotsByTower(tower)
    }

    /** @return 첫 크롤링이면 null */
    fun findLatestSnapshot(towerId: Long): TowerSnapshot? {
        return towerSnapshotQuerydslRepository.findLatestByTowerId(towerId)
    }

    fun findLatestSnapshots(towerIds: Collection<Long>): Map<Long, TowerSnapshot> {
        if (towerIds.isEmpty()) {
            return emptyMap()
        }
        return towerSnapshotQuerydslRepository.findLatestByTowerIds(towerIds).associateBy { it.tower.id.ifNullThrow() }
    }

    /** @return 변경이 없던 타워는 목록에 없음 */
    fun findLatestChangeAiSummaries(towerIds: Collection<Long>): List<LatestChangeAiSummary> {
        if (towerIds.isEmpty()) {
            return emptyList()
        }
        return towerSnapshotQuerydslRepository.findLatestChangeAiSummariesByTowerIds(towerIds)
    }

    fun findLatestChangeAiSummary(towerId: Long): String? {
        return findLatestChangeAiSummaries(listOf(towerId)).firstOrNull()?.aiSummary
    }

    fun findExpiredSnapshotsExceptLatest(createdBefore: LocalDateTime, limit: Long): List<TowerSnapshot> {
        return towerSnapshotQuerydslRepository.findExpiredExceptLatestOne(createdBefore, limit)
    }

    fun deleteSnapshots(snapshotIds: Collection<Long>, deletedAt: LocalDateTime): Long {
        return towerSnapshotQuerydslRepository.deleteByIds(snapshotIds, deletedAt)
    }
}
