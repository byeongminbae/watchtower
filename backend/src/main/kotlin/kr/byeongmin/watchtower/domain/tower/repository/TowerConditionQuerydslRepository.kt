package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.tower.entity.QTower.tower
import kr.byeongmin.watchtower.domain.tower.entity.QTowerCondition.towerCondition
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TowerConditionQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun deleteByMemberId(memberId: Long, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(towerCondition)
            .set(towerCondition.deletedAt, deletedAt)
            .set(towerCondition.updatedAt, deletedAt)
            .where(
                towerCondition.tower.id.`in`(
                    JPAExpressions.select(tower.id)
                        .from(tower)
                        .where(tower.createdBy.id.eq(memberId))
                ),
                towerCondition.deletedAt.isNull
            )
            .execute()
    }

    /**
     * 회원이 소유한 삭제되지 않은 [Tower]의 [TowerCondition] 조회
     * @return 삭제되지 않은 조건 목록. 타워가 없거나 삭제되었거나 소유자가 다르면 null, 조건이 없으면 빈 목록
     */
    fun findByTowerIdAndMemberId(towerId: Long, memberId: Long): List<TowerCondition>? {
        val rows = queryFactory
            .select(tower.id, towerCondition)
            .from(tower)
            .leftJoin(tower.conditions, towerCondition)
            .on(towerCondition.deletedAt.isNull)
            .where(
                tower.id.eq(towerId),
                tower.createdBy.id.eq(memberId),
                tower.deletedAt.isNull
            )
            .fetch()

        if (rows.isEmpty()) {
            return null
        }
        return rows.mapNotNull { it.get(towerCondition) }
    }

}
