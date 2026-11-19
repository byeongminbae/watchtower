package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.CrawlQueueClient
import kr.byeongmin.watchtower.domain.tower.dto.CrawlRequestMessageDto
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CrawlRequestProducerServiceTest {
    private val crawlQueueClient = mock<CrawlQueueClient>()
    private val towerAccessor = mock<TowerAccessor>()
    private val towerSnapshotAccessor = mock<TowerSnapshotAccessor>()
    private val transactionManager = mock<PlatformTransactionManager> {
        on { getTransaction(any()) } doReturn mock<TransactionStatus>()
    }
    private val now = LocalDateTime.of(2026, 11, 4, 1, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }
    private val crawlRequestProducerService = CrawlRequestProducerService(
        crawlQueueClient = crawlQueueClient,
        towerAccessor = towerAccessor,
        towerSnapshotAccessor = towerSnapshotAccessor,
        transactionTemplate = TransactionTemplate(transactionManager),
        timeProvider = timeProvider
    )

    private fun readyTower(towerId: Long): Tower {
        val tower = spy(
            TowerCreateRequestDto(
                name = "타워",
                url = TowerUrl.from("https://example.com"),
                fetchInterval = Duration.ofMinutes(10),
                includeInStat = true
            ).to(mock<Member>())
        ) {
            on { id } doReturn towerId
        }
        tower.changeStatusByAdmin(TowerStatus.READY)
        return tower
    }

    @Test
    fun `수집 주기가 지난 타워를 크롤링 중으로 바꾸고 자동 요청으로 큐에 넣음`() {
        /** given */
        val tower = readyTower(7L)
        whenever(towerAccessor.findCrawlScheduledTowersForUpdate(now, CrawlQueueClient.MAX_BATCH_SIZE))
            .thenReturn(listOf(tower))

        /** when */
        crawlRequestProducerService.requestScheduledCrawls()

        /** then */
        assertEquals(TowerStatus.RUNNING, tower.status)
        verify(crawlQueueClient).sendCrawlRequests(listOf(CrawlRequestMessageDto.from(tower, null, null, now)))
    }

    @Test
    fun `수집 주기가 지난 타워가 없으면 큐에 넣지 않음`() {
        /** given */
        whenever(towerAccessor.findCrawlScheduledTowersForUpdate(now, CrawlQueueClient.MAX_BATCH_SIZE))
            .thenReturn(emptyList())

        /** when */
        crawlRequestProducerService.requestScheduledCrawls()

        /** then */
        verifyNoInteractions(crawlQueueClient, towerSnapshotAccessor)
    }

    @Test
    fun `한 묶음을 가득 채워 요청하면 남은 타워를 다음 묶음으로 이어서 요청함`() {
        /** given */
        val fullBatch = (1L..CrawlQueueClient.MAX_BATCH_SIZE.toLong()).map { readyTower(it) }
        val remainingBatch = listOf(readyTower(100L))
        whenever(towerAccessor.findCrawlScheduledTowersForUpdate(now, CrawlQueueClient.MAX_BATCH_SIZE))
            .thenReturn(fullBatch, remainingBatch)

        /** when */
        crawlRequestProducerService.requestScheduledCrawls()

        /** then */
        verify(towerAccessor, times(2)).findCrawlScheduledTowersForUpdate(now, CrawlQueueClient.MAX_BATCH_SIZE)
        verify(crawlQueueClient).sendCrawlRequests(fullBatch.map { CrawlRequestMessageDto.from(it, null, null, now) })
        verify(crawlQueueClient).sendCrawlRequests(remainingBatch.map { CrawlRequestMessageDto.from(it, null, null, now) })
        verify(transactionManager, times(2)).commit(any())
    }

    @Test
    fun `큐에 넣지 못하면 그 묶음의 트랜잭션을 롤백하고 이후 묶음은 요청하지 않음`() {
        /** given */
        val tower = readyTower(7L)
        whenever(towerAccessor.findCrawlScheduledTowersForUpdate(now, CrawlQueueClient.MAX_BATCH_SIZE))
            .thenReturn(listOf(tower))
        whenever(crawlQueueClient.sendCrawlRequests(any()))
            .thenThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))

        /** when */
        assertFailsWith<BusinessException> {
            crawlRequestProducerService.requestScheduledCrawls()
        }

        /** then */
        verify(transactionManager).rollback(any())
        verify(transactionManager, never()).commit(any())
        verify(towerAccessor, times(1)).findCrawlScheduledTowersForUpdate(now, CrawlQueueClient.MAX_BATCH_SIZE)
    }

    @Test
    fun `회원이 요청하면 요청한 회원을 담아 큐에 넣음`() {
        /** given */
        val tower = readyTower(7L)
        val member = mock<Member>()

        /** when */
        crawlRequestProducerService.requestCrawls(listOf(tower), member)

        /** then */
        verify(crawlQueueClient).sendCrawlRequests(listOf(CrawlRequestMessageDto.from(tower, null, member, now)))
    }

    @Test
    fun `가장 최근 스냅샷이 있는 타워는 그 스냅샷의 텍스트와 이미지 해시를 직전 버전 해시로 담아 요청함`() {
        /** given */
        val tower = readyTower(7L)
        val latestSnapshot = mock<TowerSnapshot> {
            on { selectTextHash(any()) } doReturn "a".repeat(64)
        }
        whenever(towerSnapshotAccessor.findLatestSnapshots(listOf(7L))).thenReturn(mapOf(7L to latestSnapshot))

        /** when */
        crawlRequestProducerService.requestCrawls(listOf(tower), null)

        /** then */
        verify(crawlQueueClient).sendCrawlRequests(listOf(CrawlRequestMessageDto.from(tower, latestSnapshot, null, now)))
    }
}
