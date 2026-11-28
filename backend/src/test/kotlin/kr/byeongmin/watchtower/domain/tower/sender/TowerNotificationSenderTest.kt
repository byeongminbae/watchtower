package kr.byeongmin.watchtower.domain.tower.sender

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.entity.Notification
import kr.byeongmin.watchtower.domain.notification.service.NotificationService
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertSame

class TowerNotificationSenderTest {
    private val notificationService = mock<NotificationService>()
    private val towerNotificationSender = TowerNotificationSender(
        notificationService = notificationService,
        frontendUrl = "https://watchtower.example"
    )
    private val owner = mock<Member> {
        on { nickname } doReturn "넬리"
    }
    private val tower = spy(
        TowerCreateRequestDto(
            name = "채용 공고",
            url = TowerUrl.from("https://example.com/jobs"),
            fetchInterval = Duration.ofMinutes(10),
            includeInStat = true
        ).to(owner)
    ) {
        on { id } doReturn 9L
        on { lastFetchedAt } doReturn LocalDateTime.of(2026, 11, 22, 3, 0)
    }

    @Test
    fun `변경 알림은 타워 주인에게 Discord 마크다운으로 감지 내용과 상세 화면 링크를 보내고 저장한 알림을 돌려줌`() {
        /** given */
        val notification = mock<Notification>()
        whenever(
            notificationService.enqueueNotification(
                owner,
                "### 넬리 님! [채용 공고] 에서 방금 전 변화가 감지됐어요!\n" +
                        "> \uD83D\uDC40 **대상 페이지** <https://example.com/jobs>\n" +
                        "> \uD83D\uDD59 **감지된 시각** <t:1795316400:f>\n" +
                        "> \u2728\uFE0F **AI 요약** [TODO]\n" +
                        "> \uD83D\uDCF8 **비교 스크린샷** [TODO]\n" +
                        "https://watchtower.example/watches/9"
            )
        ).thenReturn(notification)

        /** when */
        val result = towerNotificationSender.sendChangeDetected(tower)

        /** then */
        assertSame(notification, result)
    }
}
