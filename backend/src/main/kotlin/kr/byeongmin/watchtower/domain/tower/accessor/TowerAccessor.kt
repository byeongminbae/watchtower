package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.repository.*
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TowerAccessor(
    private val towerRepository: TowerRepository,
    private val towerQuerydslRepository: TowerQuerydslRepository,
    private val detectionConditionRepository: DetectionConditionRepository,
    private val towerSnapshotQuerydslRepository: TowerSnapshotQuerydslRepository,
    private val detectionConditionQuerydslRepository: DetectionConditionQuerydslRepository
) {
    fun getTower(towerId: Long): Tower {
        return towerRepository.get(towerId)
    }

    fun findTower(towerId: Long): Tower? {
        return towerRepository.find(towerId)
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

    fun countTowers(status: TowerStatus): Long {
        return towerRepository.countByStatusAndDeletedAtIsNull(status)
    }

    fun saveTower(tower: Tower, conditions: List<DetectionCondition>): Tower {
        val savedTower = towerRepository.save(tower)
        detectionConditionRepository.saveAll(conditions)
        return savedTower
    }

    fun deleteAllOwnedTowers(memberId: Long, deletedAt: LocalDateTime) {
        towerSnapshotQuerydslRepository.deleteByMemberId(memberId, deletedAt)
        detectionConditionQuerydslRepository.deleteByMemberId(memberId, deletedAt)
        towerQuerydslRepository.deleteByMemberId(memberId, deletedAt)
    }

    /** 다른 트랜잭션이 잠근 타워는 건너뛰고, 조회한 타워는 트랜잭션이 끝날 때까지 잠김 */
    fun findCrawlScheduledTowersForUpdate(now: LocalDateTime, limit: Int): List<Tower> {
        return towerQuerydslRepository.findCrawlScheduledForUpdate(now, limit.toLong())
    }

    fun pauseTowersOfInactiveMembers(lastSignInBefore: LocalDateTime, updatedAt: LocalDateTime): Long {
        return towerQuerydslRepository.pauseByMemberLastSignInAtBefore(lastSignInBefore, updatedAt)
    }
}
