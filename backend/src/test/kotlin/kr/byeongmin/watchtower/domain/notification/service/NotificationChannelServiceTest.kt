package kr.byeongmin.watchtower.domain.notification.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationChannelAccessor
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelCreateRequestDto
import kr.byeongmin.watchtower.domain.notification.dto.NotificationChannelUpdateRequestDto
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationChannelServiceTest {
    private val memberAccessor = mock<MemberAccessor>()
    private val channelAccessor = mock<NotificationChannelAccessor>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }
    private val service = NotificationChannelService(
        memberAccessor = memberAccessor,
        channelAccessor = channelAccessor,
        timeProvider = timeProvider
    )
    private val memberId = 21L
    private val member = mock<Member>()
    private val url = "https://discord.com/api/webhooks/example"
    private val channel = spy(
        NotificationChannel.from(member, "개인 알림", NotificationChannelType.DISCORD, url)
    ) {
        on { id } doReturn 1L
    }

    @Test
    fun `회원은 자신의 알림 채널을 등록할 수 있음`() {
        /** given */
        whenever(memberAccessor.getMember(memberId)).thenReturn(member)
        whenever(channelAccessor.save(any<NotificationChannel>())).thenReturn(channel)

        /** when */
        val response = service.createChannel(
            memberId,
            NotificationChannelCreateRequestDto(
                name = "개인 알림",
                type = NotificationChannelType.DISCORD,
                webhookUrl = url
            )
        )

        /** then */
        val saved = argumentCaptor<NotificationChannel>()
        verify(channelAccessor).save(saved.capture())
        assertEquals(member, saved.firstValue.member)
        assertEquals("개인 알림", saved.firstValue.name)
        assertEquals(url, saved.firstValue.webhookUrl)
        assertEquals(NotificationChannelType.DISCORD, saved.firstValue.type)
        assertEquals(1L, response.data.id)
        assertEquals("개인 알림", response.data.name)
        assertEquals(url, response.data.webhookUrl)
    }

    @Test
    fun `존재하지 않는 회원에게 알림 채널을 등록할 수 없음`() {
        /** given */
        whenever(memberAccessor.getMember(memberId))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        assertFailsWith<BusinessException> {
            service.createChannel(
                memberId,
                NotificationChannelCreateRequestDto(
                    name = "알림",
                    type = NotificationChannelType.DISCORD,
                    webhookUrl = url
                )
            )
        }

        /** then */
        verifyNoInteractions(channelAccessor)
    }

    @Test
    fun `회원의 알림 채널을 검색해 조회 결과로 반환함`() {
        /** given */
        val pagination = CursorPaginationRequestDto(
            query = "개인",
            cursorId = 5L,
            size = 10,
            sortDirection = Sort.Direction.DESC
        )
        whenever(channelAccessor.findNotificationChannels(memberId, pagination, BaseSortField.UPDATED_AT))
            .thenReturn(listOf(channel))

        /** when */
        val response = service.getChannels(memberId, pagination, BaseSortField.UPDATED_AT).data

        /** then */
        assertEquals(1L, response.data.single().id)
        assertEquals("개인 알림", response.data.single().name)
        assertEquals(NotificationChannelType.DISCORD, response.data.single().type)
        assertEquals(url, response.data.single().webhookUrl)
    }

    @Test
    fun `회원에게 등록된 알림 채널이 없으면 빈 목록을 반환함`() {
        /** given */
        val pagination = CursorPaginationRequestDto(size = 10)
        whenever(channelAccessor.findNotificationChannels(memberId, pagination, BaseSortField.UPDATED_AT))
            .thenReturn(emptyList())

        /** when */
        val response = service.getChannels(memberId, pagination, BaseSortField.UPDATED_AT).data

        /** then */
        assertTrue(response.data.isEmpty())
        verify(channelAccessor).findNotificationChannels(memberId, pagination, BaseSortField.UPDATED_AT)
    }

    @Test
    fun `회원은 자신이 소유한 알림 채널을 조회할 수 있음`() {
        /** given */
        whenever(channelAccessor.getChannel(memberId, 1L)).thenReturn(channel)

        /** when */
        val response = service.getChannel(memberId, 1L)

        /** then */
        assertEquals(1L, response.data.id)
    }

    @Test
    fun `회원의 소유 채널을 찾을 수 없으면 조회하거나 수정할 수 없음`() {
        /** given */
        whenever(channelAccessor.getChannel(memberId, 99L))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))
        val operations: List<() -> Unit> = listOf(
            { service.getChannel(memberId, 99L) },
            { service.updateChannel(memberId, 99L, NotificationChannelUpdateRequestDto("변경", url)) }
        )

        /** when */
        val exceptions = operations.map { operation ->
            assertFailsWith<BusinessException>(block = operation)
        }

        /** then */
        exceptions.forEach { exception ->
            assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        }
    }

    @Test
    fun `알림 채널의 이름과 웹훅 주소를 바꿔도 종류와 소유자는 바뀌지 않음`() {
        /** given */
        whenever(channelAccessor.getChannel(memberId, 1L)).thenReturn(channel)

        /** when */
        val response = service.updateChannel(
            memberId,
            1L,
            NotificationChannelUpdateRequestDto(
                name = "팀 알림",
                webhookUrl = "https://discord.com/api/webhooks/team"
            )
        ).data

        /** then */
        assertEquals("팀 알림", channel.name)
        assertEquals("https://discord.com/api/webhooks/team", response.webhookUrl)
        assertEquals(NotificationChannelType.DISCORD, response.type)
        assertEquals(member, channel.member)
    }

    @Test
    fun `회원은 자신의 알림 채널 삭제를 요청할 수 있음`() {
        /** when */
        val response = service.deleteChannel(memberId, 1L)

        /** then */
        assertTrue(response.isSuccess)
        verify(channelAccessor).deleteNotificationChannel(memberId, 1L, now)
    }

    @Test
    fun `찾을 수 없는 알림 채널은 삭제할 수 없음`() {
        /** given */
        whenever(channelAccessor.deleteNotificationChannel(memberId, 99L, now))
            .thenThrow(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            service.deleteChannel(memberId, 99L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
