package kr.byeongmin.watchtower.domain.notification.accessor

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.domain.notification.repository.NotificationChannelQuerydslRepository
import kr.byeongmin.watchtower.domain.notification.repository.NotificationChannelRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerNotificationChannelQuerydslRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NotificationChannelAccessorTest {
    private val channelRepository = mock<NotificationChannelRepository>()
    private val channelQuerydslRepository = mock<NotificationChannelQuerydslRepository>()
    private val links = mock<TowerNotificationChannelQuerydslRepository>()
    private val accessor = NotificationChannelAccessor(
        notificationChannelRepository = channelRepository,
        notificationChannelQuerydslRepository = channelQuerydslRepository,
        towerNotificationChannelQuerydslRepository = links
    )
    private val channelId = 7L
    private val deletedAt = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val channel = spy(
        NotificationChannel.from(
            member = mock<Member>(),
            name = "개인 알림",
            type = NotificationChannelType.DISCORD,
            webhookUrl = "https://discord.com/api/webhooks/example"
        )
    ) {
        on { id } doReturn channelId
    }

    @Test
    fun `알림 채널을 삭제하면 타워와의 연결도 삭제 처리됨`() {
        /** when */
        accessor.deleteNotificationChannel(channel, deletedAt)

        /** then */
        assertNotNull(channel.deletedAt)
        verify(links).deleteByNotificationChannelId(channelId, deletedAt)
        verify(channelRepository, never()).delete(any<NotificationChannel>())
    }

    @Test
    fun `타워와의 연결을 정리하지 못하면 알림 채널을 삭제하지 않음`() {
        /** given */
        whenever(links.deleteByNotificationChannelId(channelId, deletedAt))
            .thenThrow(IllegalStateException("failed"))

        /** when */
        assertFailsWith<IllegalStateException> {
            accessor.deleteNotificationChannel(channel, deletedAt)
        }

        /** then */
        assertNull(channel.deletedAt)
    }
}
