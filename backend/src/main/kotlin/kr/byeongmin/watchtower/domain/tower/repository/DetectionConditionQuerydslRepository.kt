package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.tower.entity.DetectionCondition
import kr.byeongmin.watchtower.domain.tower.entity.QDetectionCondition.detectionCondition
import kr.byeongmin.watchtower.domain.tower.entity.QTower.tower
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DetectionConditionQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun deleteByMemberId(memberId: Long, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(detectionCondition)
            .set(detectionCondition.deletedAt, deletedAt)
            .set(detectionCondition.updatedAt, deletedAt)
            .where(
                detectionCondition.tower.id.`in`(
                    JPAExpressions.select(tower.id)
                        .from(tower)
                        .where(tower.createdBy.id.eq(memberId))
                ),
                detectionCondition.deletedAt.isNull
            )
            .execute()
    }

    fun findByTowerIdAndMemberId(towerId: Long, memberId: Long): List<DetectionCondition>? {
        val rows = queryFactory
            .select(tower.id, detectionCondition)
            .from(tower)
            .leftJoin(tower.conditions, detectionCondition)
            .on(detectionCondition.deletedAt.isNull)
            .where(
                tower.id.eq(towerId),
                tower.createdBy.id.eq(memberId),
                tower.deletedAt.isNull
            )
            .fetch()

        if (rows.isEmpty()) {
            return null
        }
        return rows.mapNotNull { it.get(detectionCondition) }
    }
}
