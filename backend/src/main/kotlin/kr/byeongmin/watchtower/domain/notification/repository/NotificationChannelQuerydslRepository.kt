package kr.byeongmin.watchtower.domain.notification.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.entity.QNotificationChannel.notificationChannel
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class NotificationChannelQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun deleteByMemberId(memberId: Long, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(notificationChannel)
            .set(notificationChannel.deletedAt, deletedAt)
            .set(notificationChannel.updatedAt, deletedAt)
            .where(
                notificationChannel.member.id.eq(memberId),
                notificationChannel.deletedAt.isNull
            )
            .execute()
    }

    fun findAllByCursor(
        memberId: Long,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        sortField: BaseSortField
    ): List<NotificationChannel> {
        return queryFactory
            .selectFrom(notificationChannel)
            .where(
                notificationChannel.member.id.eq(memberId),
                notificationChannel.member.deletedAt.isNull,
                notificationChannel.deletedAt.isNull,
                notificationChannel.name.startsWith(cursorPaginationRequestDto.query),
                sortField.getCursorCondition(
                    notificationChannel._super,
                    notificationChannel.id,
                    cursorPaginationRequestDto
                )
            )
            .orderBy(
                *sortField.getOrderSpecifiers(
                    notificationChannel._super,
                    notificationChannel.id,
                    cursorPaginationRequestDto
                )
            )
            .limit(cursorPaginationRequestDto.queryLimit())
            .fetch()
    }
}
