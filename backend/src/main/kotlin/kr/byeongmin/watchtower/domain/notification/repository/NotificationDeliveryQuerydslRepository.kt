package kr.byeongmin.watchtower.domain.notification.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType
import kr.byeongmin.watchtower.domain.notification.entity.NotificationDelivery
import kr.byeongmin.watchtower.domain.notification.entity.QNotificationDelivery.notificationDelivery
import org.hibernate.Timeouts
import org.hibernate.jpa.SpecHints
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class NotificationDeliveryQuerydslRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun findFirstSendableForUpdate(now: LocalDateTime): NotificationDelivery? {
        return queryFactory
            .selectFrom(notificationDelivery)
            .where(
                notificationDelivery.succeededAt.isNull,
                notificationDelivery.abandonedAt.isNull,
                notificationDelivery.deletedAt.isNull,
                notificationDelivery.nextAttemptAt.loe(now)
            )
            .orderBy(notificationDelivery.nextAttemptAt.asc())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED_MILLI)
            .fetchFirst()
    }
}
