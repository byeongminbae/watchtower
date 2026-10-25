package kr.byeongmin.watchtower.domain.tower.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import com.querydsl.jpa.JPAExpressions
import kr.byeongmin.watchtower.domain.tower.entity.QTower.tower
import kr.byeongmin.watchtower.domain.notification.entity.QNotificationChannel.notificationChannel
import kr.byeongmin.watchtower.domain.tower.entity.QTowerNotificationChannel.towerNotificationChannel
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TowerNotificationChannelQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun deleteByMemberId(memberId: Long, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(towerNotificationChannel)
            .set(towerNotificationChannel.deletedAt, deletedAt)
            .set(towerNotificationChannel.updatedAt, deletedAt)
            .where(
                towerNotificationChannel.tower.id.`in`(
                    JPAExpressions.select(tower.id)
                        .from(tower)
                        .where(tower.createdBy.id.eq(memberId))
                ).or(
                    towerNotificationChannel.notificationChannel.id.`in`(
                        JPAExpressions.select(notificationChannel.id)
                            .from(notificationChannel)
                            .where(notificationChannel.member.id.eq(memberId))
                    )
                ),
                towerNotificationChannel.deletedAt.isNull
            )
            .execute()
    }

    fun deleteByNotificationChannelId(channelId: Long, deletedAt: LocalDateTime): Long {
        return queryFactory
            .update(towerNotificationChannel)
            .set(towerNotificationChannel.deletedAt, deletedAt)
            .set(towerNotificationChannel.updatedAt, deletedAt)
            .where(
                towerNotificationChannel.notificationChannel.id.eq(channelId),
                towerNotificationChannel.deletedAt.isNull
            )
            .execute()
    }
}
