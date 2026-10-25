package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.tower.entity.QTower.tower
import kr.byeongmin.watchtower.domain.tower.entity.QTowerSnapshot.towerSnapshot
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TowerSnapshotQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    /**
     * 회원이 소유한 삭제되지 않은 타워의 [TowerSnapshot]을 조회
     * @return 삭제되지 않은 스냅샷 목록. 타워가 없거나 삭제되었거나 소유자가 다르면 null, 스냅샷이 없으면 빈 목록
     */
    fun findByTowerIdAndMemberId(
        towerId: Long,
        memberId: Long
    ): List<TowerSnapshot>? {
        val rows = queryFactory
            .select(tower.id, towerSnapshot)
            .from(tower)
            .leftJoin(tower.snapshots, towerSnapshot)
            .on(
                towerSnapshot.deletedAt.isNull
            )
            .where(
                tower.id.eq(towerId),
                tower.createdBy.id.eq(memberId),
                tower.deletedAt.isNull
            )
            .orderBy(towerSnapshot.createdAt.desc())
            .fetch()

        if (rows.isEmpty()) {
            return null
        }
        return rows.mapNotNull { it.get(towerSnapshot) }
    }

    fun deleteByCreatedAtBefore(createdAt: LocalDateTime, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(towerSnapshot)
            .set(towerSnapshot.deletedAt, deletedAt)
            .set(towerSnapshot.updatedAt, deletedAt)
            .where(
                towerSnapshot.createdAt.before(createdAt),
                towerSnapshot.deletedAt.isNull
            )
            .execute()
    }

    /**
     * 회원이 소유한 타워의 삭제되지 않은 [TowerSnapshot]을 일괄 논리 삭제
     * @param [deletedAt] 삭제 시각과 수정 시각에 함께 적용할 값
     * @return 논리 삭제한 스냅샷 수
     *
     * 이미 삭제된 타워의 스냅샷도 포함하며, 벌크 수정에는 엔티티 감사가 적용되지 않아 수정 시각도 직접 설정함
     */
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
