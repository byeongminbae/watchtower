package kr.byeongmin.watchtower.domain.tower.entity

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.entity.NotificationChannel
import kr.byeongmin.watchtower.domain.notification.enums.NotificationChannelType
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.enums.UpdatableTowerStatus
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.oci.queue.dto.CrawlResultDto
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.LocalDateTime
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

    private fun crawlResultDto(
        resultStatus: CrawlResultDto.CrawlResultStatus,
        httpStatusCode: Int? = null,
        crawlEndedAt: LocalDateTime = LocalDateTime.of(2026, 11, 3, 1, 0)
    ): CrawlResultDto {
        return CrawlResultDto(
            towerId = 7L,
            url = "https://example.com",
            scheduledAt = "2026-11-03T00:59:00.000Z",
            crawlStartedAt = crawlEndedAt.minusSeconds(5),
            crawlEndedAt = crawlEndedAt,
            resultStatus = resultStatus,
            httpStatusCode = httpStatusCode,
            finalUrl = null,
            htmlSha256 = null,
            htmlObjectName = null,
            errorCode = null,
            triggeredByMemberId = null
        )
    }

    @Test
    fun `새로 만든 타워는 첫 크롤링을 요청한 크롤링 중 상태로 시작함`() {
        /** when */
        val result = Tower.from(owner, request)

        /** then */
        assertEquals(TowerStatus.CRAWLING, result.status)
    }

    @Test
    fun `크롤링 중인 타워가 2xx 로 페이지를 받으면 다음 크롤링 대기로 바뀌고 수집 시각이 기록됨`() {
        /** given */
        val crawlEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0)

        /** when */
        tower.applyCrawlResult(
            crawlResultDto(
                CrawlResultDto.CrawlResultStatus.FETCHED,
                httpStatusCode = 200,
                crawlEndedAt = crawlEndedAt
            )
        )

        /** then */
        assertEquals(TowerStatus.CRAWLING_READY, tower.status)
        assertEquals(crawlEndedAt, tower.lastFetchedAt)
    }

    @Test
    fun `크롤링 중인 타워의 크롤링이 실패하면 크롤링 실패 상태로 바뀜`() {
        /** when */
        tower.applyCrawlResult(crawlResultDto(CrawlResultDto.CrawlResultStatus.FAILED))

        /** then */
        assertEquals(TowerStatus.CRAWLING_ERROR, tower.status)
        assertFalse(tower.isDeleted())
    }

    @ParameterizedTest
    @CsvSource("404", "503")
    fun `크롤링 중인 타워가 2xx 가 아닌 응답으로 페이지를 받으면 크롤링 실패 상태로 바뀜`(httpStatusCode: Int) {
        /** when */
        tower.applyCrawlResult(
            crawlResultDto(
                CrawlResultDto.CrawlResultStatus.FETCHED,
                httpStatusCode = httpStatusCode
            )
        )

        /** then */
        assertEquals(TowerStatus.CRAWLING_ERROR, tower.status)
    }

    @Test
    fun `크롤링 중에 회원이 일시정지한 타워는 결과가 와도 정지 상태를 유지하고 수집 시각만 기록됨`() {
        /** given */
        tower.changeStatusByMember(UpdatableTowerStatus.USER_PAUSED)
        val crawlEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0)

        /** when */
        tower.applyCrawlResult(
            crawlResultDto(
                CrawlResultDto.CrawlResultStatus.FETCHED,
                httpStatusCode = 200,
                crawlEndedAt = crawlEndedAt
            )
        )

        /** then */
        assertEquals(TowerStatus.USER_PAUSED, tower.status)
        assertEquals(crawlEndedAt, tower.lastFetchedAt)
    }

    @Test
    fun `크롤링에 실패한 타워를 재시도하면 크롤링 중으로 바뀜`() {
        /** given */
        tower.applyCrawlResult(crawlResultDto(CrawlResultDto.CrawlResultStatus.FAILED))

        /** when */
        tower.retryCrawl()

        /** then */
        assertEquals(TowerStatus.CRAWLING, tower.status)
    }

    @ParameterizedTest
    @CsvSource("CRAWLING_READY", "USER_PAUSED", "ADMIN_PAUSED")
    fun `크롤링에 실패하지 않은 타워는 재시도할 수 없음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.retryCrawl()
        }

        /** then */
        assertEquals(TowerError.CRAWL_NOT_RETRYABLE, exception.error)
        assertEquals(status, tower.status)
    }

    @Test
    fun `크롤링 중인 타워는 재시도해 크롤링 요청을 또 넣을 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.retryCrawl()
        }

        /** then */
        assertEquals(TowerError.CRAWL_NOT_RETRYABLE, exception.error)
    }

    @Test
    fun `크롤링 대기 중인 타워의 크롤링을 시작하면 크롤링 중으로 바뀜`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.CRAWLING_READY)

        /** when */
        tower.startCrawl()

        /** then */
        assertEquals(TowerStatus.CRAWLING, tower.status)
    }

    @ParameterizedTest
    @CsvSource("CRAWLING_ERROR", "USER_PAUSED", "ADMIN_PAUSED")
    fun `크롤링 대기 중이 아닌 타워는 크롤링을 시작할 수 없음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        assertFailsWith<IllegalStateException> {
            tower.startCrawl()
        }

        /** then */
        assertEquals(status, tower.status)
    }

    @Test
    fun `크롤링 중인 타워는 크롤링을 또 시작할 수 없음`() {
        /** when */
        assertFailsWith<IllegalStateException> {
            tower.startCrawl()
        }

        /** then */
        assertEquals(TowerStatus.CRAWLING, tower.status)
    }

    @Test
    fun `어드민도 크롤링 요청 없이 타워를 크롤링 중 상태로 바꿀 수 없음`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.CRAWLING_READY)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.changeStatusByAdmin(TowerStatus.CRAWLING)
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
        assertEquals(TowerStatus.CRAWLING_READY, tower.status)
    }

    @Test
    fun `전체 변경 감지 조건이 이미 있는 타워에 또 추가하면 예외가 발생함`() {
        /** given */
        HtmlCondition.from(tower)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            HtmlCondition.from(tower)
        }

        /** then */
        assertEquals(TowerError.HTML_CONDITION_DUPLICATED, exception.error)
        assertEquals(1, tower.conditions.size)
    }

    @Test
    fun `감지 조건이 없는 타워는 HTML 이 달라져도 조건을 충족하지 않음`() {
        /** when */
        val result = tower.isConditionSatisfied(previousHtml = "<p>품절</p>", currentHtml = "<p>재입고</p>")

        /** then */
        assertFalse(result)
    }

    @Test
    fun `감지 조건을 하나도 충족하지 않으면 조건을 충족하지 않음`() {
        /** given */
        KeywordCondition.from(tower, "할인", true)

        /** when */
        val result = tower.isConditionSatisfied(previousHtml = "<p>품절</p>", currentHtml = "<p>재입고</p>")

        /** then */
        assertFalse(result)
    }

    @Test
    fun `감지 조건 중 하나만 충족해도 조건을 충족함`() {
        /** given */
        KeywordCondition.from(tower, "할인", true)
        KeywordCondition.from(tower, "재입고", true)

        /** when */
        val result = tower.isConditionSatisfied(previousHtml = "<p>품절</p>", currentHtml = "<p>재입고</p>")

        /** then */
        assertTrue(result)
    }

    @Test
    fun `삭제된 감지 조건은 충족하더라도 판정에서 제외함`() {
        /** given */
        KeywordCondition.from(tower, "재입고", true).delete()

        /** when */
        val result = tower.isConditionSatisfied(previousHtml = "<p>품절</p>", currentHtml = "<p>재입고</p>")

        /** then */
        assertFalse(result)
    }

    @Test
    fun `전체 변경 감지 조건을 삭제한 타워에는 다시 추가할 수 있음`() {
        /** given */
        HtmlCondition.from(tower).delete()

        /** when */
        val condition = HtmlCondition.from(tower)

        /** then */
        assertTrue(tower.hasHtmlCondition())
        assertFalse(condition.isDeleted())
    }

    @Test
    fun `타워를 삭제하면 삭제되지 않은 스냅샷과 감지 조건도 삭제됨`() {
        val snapshot = TowerSnapshot.from(
            towerRequestHistory = mock<TowerRequestHistory> { on { tower } doReturn tower },
            htmlObjectName = "towers/7/2026-11-04T01:00:00.000Z.html",
            htmlSha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            isConditionSatisfied = false
        )
        val condition = KeywordCondition.from(tower, "재고", true)

        tower.delete()

        assertTrue(tower.isDeleted())
        assertTrue(snapshot.isDeleted())
        assertTrue(condition.isDeleted())
    }

    @Test
    fun `타워를 삭제할 때 이미 삭제된 스냅샷과 감지 조건 및 채널 연결은 건너뜀`() {
        val snapshot = TowerSnapshot.from(
            towerRequestHistory = mock<TowerRequestHistory> { on { tower } doReturn tower },
            htmlObjectName = "towers/7/2026-11-04T01:00:00.000Z.html",
            htmlSha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            isConditionSatisfied = false
        )
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

    @Test
    fun `회원은 일시정지한 자신의 타워를 크롤링 대기로 되돌릴 수 있음`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.USER_PAUSED)

        /** when */
        tower.changeStatusByMember(UpdatableTowerStatus.CRAWLING_READY)

        /** then */
        assertEquals(TowerStatus.CRAWLING_READY, tower.status)
    }

    @ParameterizedTest
    @CsvSource("CRAWLING_READY", "CRAWLING_ERROR")
    fun `회원은 크롤링 대기나 실패한 타워를 일시정지할 수 있음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        tower.changeStatusByMember(UpdatableTowerStatus.USER_PAUSED)

        /** then */
        assertEquals(TowerStatus.USER_PAUSED, tower.status)
    }

    @Test
    fun `회원이 크롤링 중인 타워를 크롤링 대기로 바꾸려 해도 결과를 반영할 수 있게 크롤링 중 상태를 유지함`() {
        /** when */
        tower.changeStatusByMember(UpdatableTowerStatus.CRAWLING_READY)

        /** then */
        assertEquals(TowerStatus.CRAWLING, tower.status)
    }

    @Test
    fun `회원이 크롤링에 실패한 타워를 크롤링 대기로 바꾸려 해도 재시도 전까지 실패 상태를 유지함`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.CRAWLING_ERROR)

        /** when */
        tower.changeStatusByMember(UpdatableTowerStatus.CRAWLING_READY)

        /** then */
        assertEquals(TowerStatus.CRAWLING_ERROR, tower.status)
    }

    @Test
    fun `회원은 관리자가 정지한 타워의 상태 변경 불가`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.ADMIN_PAUSED)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.changeStatusByMember(UpdatableTowerStatus.CRAWLING_READY)
        }

        /** then */
        assertEquals(CommonError.PERMISSION_DENIED, exception.error)
        assertEquals(TowerStatus.ADMIN_PAUSED, tower.status)
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
