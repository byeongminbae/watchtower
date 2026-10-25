package kr.byeongmin.watchtower.domain.tower.accessor

import java.time.LocalDateTime
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotQuerydslRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.stereotype.Component

@Component
class TowerSnapshotAccessor(
    private val towerSnapshotQuerydslRepository: TowerSnapshotQuerydslRepository
) {
    fun getMemberSnapshots(towerId: Long, memberId: Long): List<TowerSnapshot> {
        return towerSnapshotQuerydslRepository.findByTowerIdAndMemberId(towerId, memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun deleteByCreatedAtBefore(createdAt: LocalDateTime, deletedAt: LocalDateTime): Long {
        return towerSnapshotQuerydslRepository.deleteByCreatedAtBefore(createdAt, deletedAt)
    }
}
