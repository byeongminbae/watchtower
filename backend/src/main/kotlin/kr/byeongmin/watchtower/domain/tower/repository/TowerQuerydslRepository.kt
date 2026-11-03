package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import kr.byeongmin.watchtower.domain.member.entity.QMember.member
import kr.byeongmin.watchtower.domain.tower.entity.QTower.tower
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import org.hibernate.Timeouts
import org.hibernate.jpa.SpecHints
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
     * [now] 기준으로 크롤링이 필요한 [Tower] 를 [limit] 개만큼 조회하면서 잠금 (`FOR UPDATE SKIP LOCKED`)
     *
     * [Tower.lastFetchedAt] 이 오래된 타워부터 조회하고, 다른 트랜잭션이 이미 잠근 타워는 건너뜀. 여러 서버가 동시에 실행해도 같은 타워를 중복으로 요청하지 않기 위함
     *
     * 한 번도 수집하지 않은 타워([Tower.lastFetchedAt] 이 null)도 수집 주기가 지난 것으로 봄.
     * 수집 시각에서 주기를 빼는 계산은 Querydsl 이 지원하지 않아서 템플릿으로 작성함
     */
    fun findCrawlDueForUpdate(now: LocalDateTime, limit: Long): List<Tower> {
        return queryFactory
            .selectFrom(tower)
            .where(
                tower.status.eq(TowerStatus.CRAWLING_READY),
                tower.deletedAt.isNull,
                tower.lastFetchedAt.isNull.or(
                    Expressions.booleanTemplate("{0} <= {1} - {2}", tower.lastFetchedAt, now, tower.fetchInterval)
                )
            )
            .orderBy(tower.lastFetchedAt.asc().nullsFirst())
            .limit(limit)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED_MILLI)
            .fetch()
    }

    /**
     * 특정 시점 이후로 접속하지 않은 [kr.byeongmin.watchtower.domain.member.entity.Member] 의 크롤링 대기 중이거나 크롤링 중인 [Tower] 를 일시정지
     * 회원이 돌아와 크롤링 대기로 되돌릴 수 있게 [TowerStatus.USER_PAUSED] 로 바꿈
     */
    fun pauseByMemberLastSignInAtBefore(lastSignInBefore: LocalDateTime, updatedAt: LocalDateTime): Long {
        return queryFactory
            .update(tower)
            .set(tower.status, TowerStatus.USER_PAUSED)
            .set(tower.updatedAt, updatedAt)
            .where(
                tower.status.`in`(TowerStatus.CRAWLING_READY, TowerStatus.CRAWLING),
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
