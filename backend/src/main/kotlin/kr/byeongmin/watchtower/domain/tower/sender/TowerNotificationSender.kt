package kr.byeongmin.watchtower.domain.tower.sender

import kr.byeongmin.watchtower.domain.notification.entity.Notification
import kr.byeongmin.watchtower.domain.notification.service.NotificationService
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class TowerNotificationSender(
    private val notificationService: NotificationService,
    @Value("\${watchtower.frontend-url}") private val frontendUrl: String
) {
    fun sendChangeDetected(tower: Tower): Notification {
        return notificationService.enqueueNotification(
            recipient = tower.createdBy,
            content = "### ${tower.createdBy.nickname} 님! [${tower.name}] 에서 방금 전 변화가 감지됐어요!\n" +
                    "> \uD83D\uDC40 **대상 페이지** <${tower.url}>\n" +
                    "> \uD83D\uDD59 **감지된 시각** <t:${
                        tower.lastFetchedAt.ifNullThrow().toEpochSecond(ZoneOffset.UTC)
                    }:f>\n" +
                    "> \u2728\uFE0F **AI 요약** [TODO]\n" + // TODO
                    "> \uD83D\uDCF8 **비교 스크린샷** [TODO]\n" + // TODO
                    "$frontendUrl/watches/${tower.id}"
        )
    }
}
