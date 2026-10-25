package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition
import kr.byeongmin.watchtower.domain.tower.entity.TowerNotificationChannel
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.repository.*
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TowerAccessor(
    private val towerRepository: TowerRepository,
    private val towerQuerydslRepository: TowerQuerydslRepository,
    private val towerConditionRepository: TowerConditionRepository,
    private val towerNotificationChannelRepository: TowerNotificationChannelRepository,
    private val towerSnapshotQuerydslRepository: TowerSnapshotQuerydslRepository,
    private val towerConditionQuerydslRepository: TowerConditionQuerydslRepository,
    private val towerNotificationChannelQuerydslRepository: TowerNotificationChannelQuerydslRepository
) {
    fun getTower(towerId: Long): Tower {
        return towerRepository.get(towerId)
    }

    fun getOwnedTower(towerId: Long, memberId: Long): Tower {
        return towerRepository.findByIdAndCreatedBy_IdAndDeletedAtIsNull(towerId, memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun getOwnedTowerForUpdate(towerId: Long, memberId: Long): Tower {
        return towerRepository.findTowerForUpdateByIdAndCreatedBy_IdAndDeletedAtIsNull(towerId, memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun findTowers(offsetPaginationRequestDto: OffsetPaginationRequestDto, sortField: BaseSortField): Page<Tower> {
        return towerRepository.findByNameStartingWith(
            offsetPaginationRequestDto.query,
            offsetPaginationRequestDto.to(sortField)
        )
    }

    fun findOwnedTowers(
        memberId: Long,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        sortField: BaseSortField
    ): List<Tower> {
        return towerQuerydslRepository.findAllByCursor(memberId, cursorPaginationRequestDto, sortField)
    }

    fun countOwnedTowers(memberId: Long): Long {
        return towerRepository.countByCreatedBy_IdAndDeletedAtIsNull(memberId)
    }

    fun countByStatus(status: TowerStatus): Long {
        return towerRepository.countByStatusAndDeletedAtIsNull(status)
    }

    fun saveTower(
        tower: Tower,
        conditions: List<TowerCondition>,
        notificationChannels: List<TowerNotificationChannel>
    ) {
        towerRepository.save(tower)
        if (notificationChannels.isNotEmpty()) {
            towerNotificationChannelRepository.saveAll(notificationChannels)
        }
        towerConditionRepository.saveAll(conditions)
    }

    fun deleteAllRelatedByMemberId(memberId: Long, deletedAt: LocalDateTime) {
        towerSnapshotQuerydslRepository.deleteByMemberId(memberId, deletedAt)
        towerConditionQuerydslRepository.deleteByMemberId(memberId, deletedAt)
        towerNotificationChannelQuerydslRepository.deleteByMemberId(memberId, deletedAt)
        towerQuerydslRepository.deleteByMemberId(memberId, deletedAt)
    }

    fun pauseByMemberLastSignInAtBefore(lastSignInBefore: LocalDateTime, updatedAt: LocalDateTime): Long {
        return towerQuerydslRepository.pauseByMemberLastSignInAtBefore(lastSignInBefore, updatedAt)
    }
}
