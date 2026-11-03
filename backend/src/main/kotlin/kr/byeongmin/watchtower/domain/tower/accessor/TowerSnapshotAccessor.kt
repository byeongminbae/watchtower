package kr.byeongmin.watchtower.domain.tower.accessor

import java.time.LocalDateTime
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotQuerydslRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class TowerSnapshotAccessor(
    private val towerSnapshotRepository: TowerSnapshotRepository,
    private val towerSnapshotQuerydslRepository: TowerSnapshotQuerydslRepository
) {
    fun save(towerSnapshot: TowerSnapshot): TowerSnapshot {
        return towerSnapshotRepository.save(towerSnapshot)
    }

    fun getMemberSnapshots(towerId: Long, memberId: Long): List<TowerSnapshot> {
        return towerSnapshotQuerydslRepository.findByTowerIdAndMemberId(towerId, memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    /** 새 크롤링 결과와 비교할 직전 버전. 첫 크롤링이면 null */
    fun findLatestSnapshot(towerId: Long): TowerSnapshot? {
        return towerSnapshotQuerydslRepository.findLatestByTowerId(towerId)
    }

    /** @return 타워 id 별 마지막 변경의 AI 요약. 변경이 없던 타워는 키가 없음 */
    fun findLatestChangeAiSummaries(towerIds: Collection<Long>): Map<Long, String?> {
        if (towerIds.isEmpty()) {
            return emptyMap()
        }
        return towerSnapshotQuerydslRepository.findLatestChangeAiSummaries(towerIds)
    }

    fun deleteByCreatedAtBefore(createdAt: LocalDateTime, deletedAt: LocalDateTime): Long {
        return towerSnapshotQuerydslRepository.deleteByCreatedAtBefore(createdAt, deletedAt)
    }
}
