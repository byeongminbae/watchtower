package kr.byeongmin.watchtower.domain.tower.entity

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.enums.UpdatableTowerStatus
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TowerTest {
    private val plan = mock<Plan> {
        on { maxConditionQuantity } doReturn 3
    }
    private val owner = mock<Member> {
        on { plan } doReturn plan
    }
    private val request = TowerCreateRequestDto(
        name = "타워",
        url = TowerUrl.from("https://example.com"),
        fetchInterval = Duration.ofMinutes(10),
        includeInStat = true
    )
    private val tower = Tower.from(owner, request)

    @Test
    fun `새로 만든 타워는 실행 중 상태로 시작함`() {
        /** when */
        val result = Tower.from(owner, request)

        /** then */
        assertEquals(TowerStatus.RUNNING, result.status)
    }

    @Test
    fun `타워를 삭제하면 삭제되지 않은 스냅샷과 감지 조건도 삭제됨`() {
        val snapshot = TowerSnapshot.from(tower, "https://example.com/snapshot")
        val condition = KeywordCondition.from(tower, "재고", true)

        tower.delete()

        assertTrue(tower.isDeleted())
        assertTrue(snapshot.isDeleted())
        assertTrue(condition.isDeleted())
    }

    @Test
    fun `타워를 삭제할 때 이미 삭제된 스냅샷과 감지 조건 및 채널 연결은 건너뜀`() {
        val snapshot = TowerSnapshot.from(tower, "https://example.com/snapshot")
        val condition = HtmlCondition.from(tower)
        val channel = NotificationChannel.from(
            member = owner,
            name = "개인 알림",
            type = NotificationChannelType.DISCORD,
            webhookUrl = "https://discord.com/api/webhooks/example"
        )
        val link = TowerNotificationChannel.of(tower, channel)
        snapshot.delete()
        condition.delete()
        link.delete()
        val snapshotDeletedAt = snapshot.deletedAt
        val conditionDeletedAt = condition.deletedAt
        val linkDeletedAt = link.deletedAt

        tower.delete()

        assertTrue(tower.isDeleted())
        assertEquals(snapshotDeletedAt, snapshot.deletedAt)
        assertEquals(conditionDeletedAt, condition.deletedAt)
        assertEquals(linkDeletedAt, link.deletedAt)
    }

    @Test
    fun `타워를 삭제해도 알림 채널은 남고 연결만 삭제 처리됨`() {
        val channel = NotificationChannel.from(
            member = owner,
            name = "개인 알림",
            type = NotificationChannelType.DISCORD,
            webhookUrl = "https://discord.com/api/webhooks/example"
        )
        val link = TowerNotificationChannel.of(tower, channel)

        tower.delete()

        assertTrue(link.isDeleted())
        assertFalse(channel.isDeleted())
    }

    @Test
    fun `타워의 이름과 수집 주기 및 통계 포함 여부를 변경할 수 있음`() {
        /** when */
        tower.changeFetchInterval(Duration.ofMinutes(30))
        tower.rename("변경된-타워-이름")
        tower.changeStatInclusion(false)

        /** then */
        assertEquals("변경된-타워-이름", tower.name)
        assertEquals(Duration.ofSeconds(1800), tower.fetchInterval)
        assertFalse(tower.includeInStat)
    }

    @ParameterizedTest
    @CsvSource("RUNNING,PAUSED", "PAUSED,PAUSED", "ILLEGAL_SUSPENDED,ILLEGAL_SUSPENDED")
    fun `실행 중인 타워만 일시정지되고 이미 정지된 타워의 상태는 유지됨`(before: TowerStatus, after: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(before)

        /** when */
        tower.pause()

        /** then */
        assertEquals(after, tower.status)
    }

    @Test
    fun `회원은 일시정지한 자신의 타워를 다시 실행할 수 있음`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.PAUSED)

        /** when */
        tower.changeStatusByMember(UpdatableTowerStatus.RUNNING)

        /** then */
        assertEquals(TowerStatus.RUNNING, tower.status)
    }

    @Test
    fun `회원은 정책 위반으로 정지된 타워의 상태 변경 불가`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.ILLEGAL_SUSPENDED)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.changeStatusByMember(UpdatableTowerStatus.RUNNING)
        }

        /** then */
        assertEquals(CommonError.PERMISSION_DENIED, exception.error)
        assertEquals(TowerStatus.ILLEGAL_SUSPENDED, tower.status)
    }

    @Test
    fun `감지 조건은 이용 중인 플랜의 한도까지 추가할 수 있음`() {
        /** when */
        val isAddable = tower.isConditionAddable(currentQuantity = 2, additionalQuantity = 1)

        /** then */
        assertTrue(isAddable)
    }

    @Test
    fun `감지 조건을 추가한 개수가 플랜 한도를 초과할 수 없음`() {
        /** when */
        val isAddable = tower.isConditionAddable(currentQuantity = 3, additionalQuantity = 1)

        /** then */
        assertFalse(isAddable)
    }
}
