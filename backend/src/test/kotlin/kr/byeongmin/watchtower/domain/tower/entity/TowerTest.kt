package kr.byeongmin.watchtower.domain.tower.entity

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import kr.byeongmin.watchtower.domain.tower.enums.PresenceChange
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.domain.tower.vo.CssSelector
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.domain.tower.vo.Keyword
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.*

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
    private val tower = request.to(owner)

    @Test
    fun `새로 만든 타워는 첫 크롤링을 요청한 크롤링 중 상태로 시작함`() {
        /** when */
        val result = request.to(owner)

        /** then */
        assertEquals(TowerStatus.RUNNING, result.status)
    }

    @Test
    fun `크롤링 중인 타워가 2xx 로 페이지를 받으면 다음 크롤링 대기로 바뀌고 수집 시각이 기록됨`() {
        /** given */
        val crawlEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0)

        /** when */
        tower.recordCrawlResult(crawlingEndedAt = crawlEndedAt, crawlerResultCode = CrawlerResultCode.FETCHED, isCrawlingSucceed = true)

        /** then */
        assertEquals(TowerStatus.READY, tower.status)
        assertEquals(crawlEndedAt, tower.lastFetchedAt)
    }

    @Test
    fun `크롤링 중인 타워의 크롤링이 실패하면 크롤링 실패 상태로 바뀌고 실패 이유가 기록됨`() {
        /** when */
        tower.recordCrawlResult(
            crawlingEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0),
            crawlerResultCode = CrawlerResultCode.NAVIGATION_TIMEOUT,
            isCrawlingSucceed = false
        )

        /** then */
        assertEquals(TowerStatus.ERROR, tower.status)
        assertEquals(CrawlerResultCode.NAVIGATION_TIMEOUT, tower.lastCrawlerResultCode)
        assertFalse(tower.isDeleted())
    }

    @Test
    fun `크롤링 중에 회원이 일시정지한 타워는 결과가 와도 정지 상태를 유지하고 수집 시각만 기록됨`() {
        /** given */
        tower.pause()
        val crawlEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0)

        /** when */
        tower.recordCrawlResult(crawlingEndedAt = crawlEndedAt, crawlerResultCode = CrawlerResultCode.FETCHED, isCrawlingSucceed = true)

        /** then */
        assertEquals(TowerStatus.PAUSED, tower.status)
        assertEquals(crawlEndedAt, tower.lastFetchedAt)
    }

    @Test
    fun `크롤링에 실패한 타워를 재시도하면 크롤링 중으로 바뀜`() {
        /** given */
        tower.recordCrawlResult(
            crawlingEndedAt = LocalDateTime.of(2026, 11, 3, 1, 0),
            crawlerResultCode = CrawlerResultCode.NAVIGATION_TIMEOUT,
            isCrawlingSucceed = false
        )

        /** when */
        tower.markCrawlRetryRequested()

        /** then */
        assertEquals(TowerStatus.RUNNING, tower.status)
    }

    @ParameterizedTest
    @CsvSource("READY", "PAUSED", "SUSPENDED")
    fun `크롤링에 실패하지 않은 타워는 재시도할 수 없음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.markCrawlRetryRequested()
        }

        /** then */
        assertEquals(TowerError.CRAWL_NOT_RETRYABLE, exception.error)
        assertEquals(status, tower.status)
    }

    @Test
    fun `크롤링 중인 타워는 재시도해 크롤링 요청을 또 넣을 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.markCrawlRetryRequested()
        }

        /** then */
        assertEquals(TowerError.CRAWL_NOT_RETRYABLE, exception.error)
    }

    @Test
    fun `크롤링 대기 중인 타워의 크롤링을 시작하면 크롤링 중으로 바뀜`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.READY)

        /** when */
        tower.markCrawlRequested()

        /** then */
        assertEquals(TowerStatus.RUNNING, tower.status)
    }

    @ParameterizedTest
    @CsvSource("ERROR", "PAUSED", "SUSPENDED")
    fun `크롤링 대기 중이 아닌 타워는 크롤링을 시작할 수 없음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.markCrawlRequested()
        }

        /** then */
        assertEquals(TowerError.CRAWL_NOT_REQUESTABLE, exception.error)
        assertEquals(status, tower.status)
    }

    @Test
    fun `크롤링 중인 타워는 크롤링을 또 시작할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.markCrawlRequested()
        }

        /** then */
        assertEquals(TowerError.CRAWL_NOT_REQUESTABLE, exception.error)
        assertEquals(TowerStatus.RUNNING, tower.status)
    }

    @Test
    fun `어드민도 크롤링 요청 없이 타워를 크롤링 중 상태로 바꿀 수 없음`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.READY)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.changeStatusByAdmin(TowerStatus.RUNNING)
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
        assertEquals(TowerStatus.READY, tower.status)
    }

    @Test
    fun `텍스트 변화 감지를 켠 타워는 텍스트가 달라지면 변화를 감지함`() {
        /** when */
        val result = tower.detectsChange(previous = textContent("품절"), current = textContent("재입고"))

        /** then */
        assertTrue(result)
    }

    @Test
    fun `텍스트 변화 감지를 끄고 감지 조건도 없는 타워는 텍스트가 달라져도 변화를 감지하지 않음`() {
        /** given */
        val keywordOnlyTower = request.copy(detectTextChange = false).to(owner)

        /** when */
        val result = keywordOnlyTower.detectsChange(previous = textContent("품절"), current = textContent("재입고"))

        /** then */
        assertFalse(result)
    }

    @Test
    fun `이미지 변화 감지를 켠 타워는 이미지만 달라져도 변화를 감지함`() {
        /** given */
        val imageTower = request.copy(detectTextChange = false, detectImageChange = true).to(owner)

        /** when */
        val result = imageTower.detectsChange(
            previous = imageContent("a".repeat(64)),
            current = imageContent("b".repeat(64))
        )

        /** then */
        assertTrue(result)
    }

    @Test
    fun `이미지 변화 감지를 끈 타워는 이미지가 달라져도 변화를 감지하지 않음`() {
        /** given */
        val textOnlyTower = request.copy(detectImageChange = false).to(owner)

        /** when */
        val result = textOnlyTower.detectsChange(
            previous = imageContent("a".repeat(64)),
            current = imageContent("b".repeat(64))
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `감지 조건을 하나도 충족하지 않으면 변화를 감지하지 않음`() {
        /** given */
        val keywordOnlyTower = request.copy(detectTextChange = false).to(owner)
        KeywordCondition.from(keywordOnlyTower, Keyword("할인"), PresenceChange.APPEAR)

        /** when */
        val result = keywordOnlyTower.detectsChange(previous = textContent("품절"), current = textContent("재입고"))

        /** then */
        assertFalse(result)
    }

    @Test
    fun `감지 조건 중 하나만 충족해도 변화를 감지함`() {
        /** given */
        val keywordOnlyTower = request.copy(detectTextChange = false).to(owner)
        KeywordCondition.from(keywordOnlyTower, Keyword("할인"), PresenceChange.APPEAR)
        KeywordCondition.from(keywordOnlyTower, Keyword("재입고"), PresenceChange.APPEAR)

        /** when */
        val result = keywordOnlyTower.detectsChange(previous = textContent("품절"), current = textContent("재입고"))

        /** then */
        assertTrue(result)
    }

    @Test
    fun `삭제된 감지 조건은 충족하더라도 판정에서 제외함`() {
        /** given */
        val keywordOnlyTower = request.copy(detectTextChange = false).to(owner)
        KeywordCondition.from(keywordOnlyTower, Keyword("재입고"), PresenceChange.APPEAR).delete()

        /** when */
        val result = keywordOnlyTower.detectsChange(previous = textContent("품절"), current = textContent("재입고"))

        /** then */
        assertFalse(result)
    }

    @Test
    fun `텍스트와 이미지 변화 감지를 모두 끄고 감지 조건도 없는 타워는 저장할 수 없음`() {
        /** given */
        val nothingDetectingTower = request.copy(detectTextChange = false, detectImageChange = false).to(owner)

        /** when */
        val exception = assertFailsWith<BusinessException> { nothingDetectingTower.throwIfNothingToDetect() }

        /** then */
        assertEquals(TowerError.HAS_NO_DETECTION, exception.error)
    }

    @Test
    fun `감지 옵션을 모두 끈 타워는 남은 감지 조건을 모두 삭제하면 저장할 수 없음`() {
        /** given */
        val keywordOnlyTower = request.copy(detectTextChange = false).to(owner)
        KeywordCondition.from(keywordOnlyTower, Keyword("재입고"), PresenceChange.APPEAR).delete()

        /** when */
        val exception = assertFailsWith<BusinessException> { keywordOnlyTower.throwIfNothingToDetect() }

        /** then */
        assertEquals(TowerError.HAS_NO_DETECTION, exception.error)
    }

    @Test
    fun `감지 옵션을 모두 꺼도 감지 조건이 하나라도 있으면 저장할 수 있음`() {
        /** given */
        val keywordOnlyTower = request.copy(detectTextChange = false).to(owner)
        KeywordCondition.from(keywordOnlyTower, Keyword("재입고"), PresenceChange.APPEAR)

        /** when, then */
        keywordOnlyTower.throwIfNothingToDetect()
    }

    @Test
    fun `감시 영역을 바꾼 뒤의 첫 비교는 텍스트가 달라져도 변화를 감지하지 않음`() {
        /** when */
        val result = tower.detectsChange(
            previous = textContent("품절", detectionAreaSelector = null),
            current = textContent("재입고", detectionAreaSelector = ".stock")
        )

        /** then */
        assertFalse(result)
    }

    @Test
    fun `감지 범위 선택자를 지정했다가 null 로 바꾸면 페이지 전체를 감시함`() {
        /** given */
        tower.changeDetectionAreaSelector(CssSelector(".stock"))

        /** when */
        tower.changeDetectionAreaSelector(null)

        /** then */
        assertNull(tower.detectionAreaSelector)
    }

    @Test
    fun `타워를 삭제하면 삭제되지 않은 스냅샷과 감지 조건도 삭제됨`() {
        val snapshot = snapshot()
        val condition = KeywordCondition.from(tower, Keyword("재고"), PresenceChange.APPEAR)

        tower.delete()

        assertTrue(tower.isDeleted())
        assertTrue(snapshot.isDeleted())
        assertTrue(condition.isDeleted())
    }

    @Test
    fun `타워를 삭제할 때 이미 삭제된 스냅샷과 감지 조건은 건너뜀`() {
        val snapshot = snapshot()
        val condition = KeywordCondition.from(tower, Keyword("재고"), PresenceChange.APPEAR)
        snapshot.delete()
        condition.delete()
        val snapshotDeletedAt = snapshot.deletedAt
        val conditionDeletedAt = condition.deletedAt

        tower.delete()

        assertTrue(tower.isDeleted())
        assertEquals(snapshotDeletedAt, snapshot.deletedAt)
        assertEquals(conditionDeletedAt, condition.deletedAt)
    }

    @Test
    fun `회원은 일시정지한 타워를 다시 시작해 크롤링 대기로 되돌릴 수 있음`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.PAUSED)

        /** when */
        tower.resume()

        /** then */
        assertEquals(TowerStatus.READY, tower.status)
    }

    @Test
    fun `크롤링 중인 타워는 다시 시작할 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.resume()
        }

        /** then */
        assertEquals(TowerError.NOT_RESUMABLE, exception.error)
        assertEquals(TowerStatus.RUNNING, tower.status)
    }

    @ParameterizedTest
    @CsvSource("READY", "ERROR", "SUSPENDED")
    fun `크롤링 대기나 실패했거나 관리자가 정지한 타워는 다시 시작할 수 없음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.resume()
        }

        /** then */
        assertEquals(TowerError.NOT_RESUMABLE, exception.error)
        assertEquals(status, tower.status)
    }

    @Test
    fun `회원은 크롤링 중인 타워도 일시정지할 수 있음`() {
        /** when */
        tower.pause()

        /** then */
        assertEquals(TowerStatus.PAUSED, tower.status)
    }

    @ParameterizedTest
    @CsvSource("READY", "ERROR")
    fun `회원은 크롤링 대기나 실패한 타워를 일시정지할 수 있음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        tower.pause()

        /** then */
        assertEquals(TowerStatus.PAUSED, tower.status)
    }

    @ParameterizedTest
    @CsvSource("PAUSED", "SUSPENDED")
    fun `이미 일시정지했거나 관리자가 정지한 타워는 일시정지할 수 없음`(status: TowerStatus) {
        /** given */
        tower.changeStatusByAdmin(status)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.pause()
        }

        /** then */
        assertEquals(TowerError.NOT_PAUSABLE, exception.error)
        assertEquals(status, tower.status)
    }

    @Test
    fun `회원은 관리자가 정지한 타워를 수정할 수 없음`() {
        /** given */
        tower.changeStatusByAdmin(TowerStatus.SUSPENDED)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.throwIfNotMemberChangeable()
        }

        /** then */
        assertEquals(CommonError.PERMISSION_DENIED, exception.error)
    }

    @Test
    fun `감지 조건은 이용 중인 플랜의 한도까지 추가할 수 있음`() {
        /** given */
        repeat(3) { KeywordCondition.from(tower, Keyword("재고$it"), PresenceChange.APPEAR) }

        /** when */
        val result = runCatching { tower.throwIfConditionQuotaExceeded() }

        /** then */
        assertTrue(result.isSuccess)
    }

    @Test
    fun `감지 조건을 추가한 개수가 플랜 한도를 초과할 수 없음`() {
        /** given */
        repeat(4) { KeywordCondition.from(tower, Keyword("재고$it"), PresenceChange.APPEAR) }

        /** when */
        val exception = assertFailsWith<BusinessException> {
            tower.throwIfConditionQuotaExceeded()
        }

        /** then */
        assertEquals(TowerError.CONDITION_QUOTA_EXCEEDED, exception.error)
    }

    @Test
    fun `삭제한 감지 조건은 플랜 한도에 세지 않음`() {
        /** given */
        val conditions = List(4) { KeywordCondition.from(tower, Keyword("재고$it"), PresenceChange.APPEAR) }
        conditions.first().delete()

        /** when */
        val result = runCatching { tower.throwIfConditionQuotaExceeded() }

        /** then */
        assertTrue(result.isSuccess)
    }

    /** 텍스트만 비교하는 조건용 내용. 해시는 텍스트마다 다른 값이면 충분함 */
    private fun textContent(text: String, detectionAreaSelector: String? = null): DetectionContent {
        return DetectionContent(
            textHash = "hash-of-$text",
            detectionAreaSelector = detectionAreaSelector,
            imageHash = null,
            textLoader = { text }
        )
    }

    /** 텍스트는 같고 이미지만 다른 내용 */
    private fun imageContent(imageHash: String): DetectionContent {
        return DetectionContent(
            textHash = "hash-of-text",
            detectionAreaSelector = null,
            imageHash = imageHash,
            textLoader = { "text" }
        )
    }

    private fun snapshot(): TowerSnapshot {
        return TowerSnapshot.of(
            towerRequestHistory = mock<TowerRequestHistory> { on { tower } doReturn tower },
            textObjectName = "towers/7/2026-11-04T01:00:00.000Z.txt",
            textHash = "a".repeat(64),
            dateTimeIgnoredTextHash = "d".repeat(64),
            detectionAreaSelector = null,
            imageHash = null,
            isChangeDetected = false
        )
    }
}
