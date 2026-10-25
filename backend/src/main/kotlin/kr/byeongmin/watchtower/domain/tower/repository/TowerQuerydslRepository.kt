package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.member.entity.QMember.member
import kr.byeongmin.watchtower.domain.tower.entity.QTower.tower
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TowerQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    /**
     * 커서 페이지네이션으로 회원이 소유한 삭제되지 않은 [Tower] 조회
     * @return 다음 페이지 존재 여부 확인을 위해 요청 크기보다 최대 한 건 더 포함한 목록
     */
    fun findAllByCursor(
        memberId: Long,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        sortField: BaseSortField
    ): List<Tower> {
        return queryFactory
            .selectFrom(tower)
            .where(
                tower.createdBy.id.eq(memberId),
                tower.name.startsWith(cursorPaginationRequestDto.query),
                tower.deletedAt.isNull,
                sortField.getCursorCondition(tower._super, tower.id, cursorPaginationRequestDto)
            )
            .orderBy(*sortField.getOrderSpecifiers(tower._super, tower.id, cursorPaginationRequestDto))
            .limit(cursorPaginationRequestDto.queryLimit())
            .fetch()
    }

    /**
     * 특정 시점 이후로 접속하지 않은 [kr.byeongmin.watchtower.domain.member.entity.Member] 의 [Tower] 를 일시정지
     */
    fun pauseByMemberLastSignInAtBefore(lastSignInBefore: LocalDateTime, updatedAt: LocalDateTime): Long {
        return queryFactory
            .update(tower)
            .set(tower.status, TowerStatus.PAUSED)
            .set(tower.updatedAt, updatedAt)
            .where(
                tower.status.eq(TowerStatus.RUNNING),
                tower.deletedAt.isNull,
                tower.createdBy.id.`in`(
                    JPAExpressions.select(member.id)
                        .from(member)
                        .where(member.lastSignInAt.before(lastSignInBefore))
                )
            )
            .execute()
    }

    /**
     * 회원이 소유한 삭제되지 않은 [Tower]를 일괄 논리 삭제
     * @param [deletedAt] 삭제 시각과 수정 시각에 함께 적용할 값
     * @return 논리 삭제한 타워 수
     */
    fun deleteByMemberId(memberId: Long, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(tower)
            .set(tower.deletedAt, deletedAt)
            .set(tower.updatedAt, deletedAt)
            .where(
                tower.createdBy.id.eq(memberId),
                tower.deletedAt.isNull
            )
            .execute()
    }

}
