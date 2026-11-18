package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.JPQLQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.tower.entity.QTower.tower
import kr.byeongmin.watchtower.domain.tower.entity.QTowerSnapshot
import kr.byeongmin.watchtower.domain.tower.entity.QTowerSnapshot.towerSnapshot
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.vo.LatestChangeAiSummary
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TowerSnapshotQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findChangedSnapshotsByTower(tower: Tower): List<TowerSnapshot> {
        return queryFactory
            .selectFrom(towerSnapshot)
            .where(
                towerSnapshot.tower.eq(tower),
                towerSnapshot.isChangeDetected.isTrue,
                towerSnapshot.deletedAt.isNull
            )
            .orderBy(towerSnapshot.createdAt.desc())
            .fetch()
    }

    fun findLatestChangeAiSummariesByTowerIds(towerIds: Collection<Long>): List<LatestChangeAiSummary> {
        return queryFactory
            .select(towerSnapshot.tower.id, towerSnapshot.aiDiffSummary)
            .from(towerSnapshot)
            .where(
                towerSnapshot.id.`in`(
                    JPAExpressions
                        .select(towerSnapshot.id.max())
                        .from(towerSnapshot)
                        .where(
                            towerSnapshot.tower.id.`in`(towerIds),
                            towerSnapshot.isChangeDetected.isTrue,
                            towerSnapshot.deletedAt.isNull
                        )
                        .groupBy(towerSnapshot.tower.id)
                )
            )
            .fetch()
            .map {
                LatestChangeAiSummary(
                    towerId = it.get(towerSnapshot.tower.id).ifNullThrow(),
                    aiSummary = it.get(towerSnapshot.aiDiffSummary)
                )
            }
    }

    fun findLatestByTowerId(towerId: Long): TowerSnapshot? {
        return queryFactory
            .selectFrom(towerSnapshot)
            .where(
                towerSnapshot.tower.id.eq(towerId),
                towerSnapshot.deletedAt.isNull
            )
            .orderBy(towerSnapshot.createdAt.desc())
            .fetchFirst()
    }

    fun findLatestByTowerIds(towerIds: Collection<Long>): List<TowerSnapshot> {
        return queryFactory
            .selectFrom(towerSnapshot)
            .where(
                towerSnapshot.tower.id.`in`(towerIds),
                towerSnapshot.deletedAt.isNull,
                towerSnapshot.createdAt.eq(latestCreatedAtOfSameTower())
            )
            .fetch()
    }

    fun findExpiredExceptLatestOne(createdAt: LocalDateTime, limit: Long): List<TowerSnapshot> {
        return queryFactory
            .selectFrom(towerSnapshot)
            .where(
                towerSnapshot.createdAt.before(createdAt),
                towerSnapshot.deletedAt.isNull,
                towerSnapshot.createdAt.before(latestCreatedAtOfSameTower())
            )
            .orderBy(towerSnapshot.id.asc())
            .limit(limit)
            .fetch()
    }

    private fun latestCreatedAtOfSameTower(): JPQLQuery<LocalDateTime> {
        val sameTowerSnapshot = QTowerSnapshot("sameTowerSnapshot")
        return JPAExpressions
            .select(sameTowerSnapshot.createdAt.max())
            .from(sameTowerSnapshot)
            .where(
                sameTowerSnapshot.tower.id.eq(towerSnapshot.tower.id),
                sameTowerSnapshot.deletedAt.isNull
            )
    }

    fun deleteByIds(towerSnapshotIds: Collection<Long>, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(towerSnapshot)
            .set(towerSnapshot.deletedAt, deletedAt)
            .set(towerSnapshot.updatedAt, deletedAt)
            .where(
                towerSnapshot.id.`in`(towerSnapshotIds),
                towerSnapshot.deletedAt.isNull
            )
            .execute()
    }

    fun deleteByMemberId(memberId: Long, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(towerSnapshot)
            .set(towerSnapshot.deletedAt, deletedAt)
            .set(towerSnapshot.updatedAt, deletedAt)
            .where(
                towerSnapshot.tower.id.`in`(
                    JPAExpressions
                        .select(tower.id)
                        .from(tower)
                        .where(tower.createdBy.id.eq(memberId))
                ),
                towerSnapshot.deletedAt.isNull
            )
            .execute()
    }

}
