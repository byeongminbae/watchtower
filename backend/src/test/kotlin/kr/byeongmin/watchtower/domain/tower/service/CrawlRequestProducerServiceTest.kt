package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.oci.queue.client.OciQueueClient
import kr.byeongmin.watchtower.external.oci.queue.dto.CrawlRequestDto
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
    private val ociQueueClient = mock<OciQueueClient>()
    private val towerAccessor = mock<TowerAccessor>()
    private val transactionManager = mock<PlatformTransactionManager> {
        on { getTransaction(any()) } doReturn mock<TransactionStatus>()
    }
    private val now = LocalDateTime.of(2026, 11, 4, 1, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }
    private val crawlRequestProducerService = CrawlRequestProducerService(
        ociQueueClient = ociQueueClient,
        towerAccessor = towerAccessor,
        transactionTemplate = TransactionTemplate(transactionManager),
        timeProvider = timeProvider
    )

    private fun readyTower(towerId: Long): Tower {
        val tower = spy(
            Tower.from(
                mock<Member>(),
                TowerCreateRequestDto(
                    name = "타워",
                    url = TowerUrl.from("https://example.com"),
                    fetchInterval = Duration.ofMinutes(10),
                    includeInStat = true
                )
            )
        ) {
            on { id } doReturn towerId
        }
        tower.changeStatusByAdmin(TowerStatus.CRAWLING_READY)
        return tower
    }

    @Test
    fun `수집 주기가 지난 타워를 크롤링 중으로 바꾸고 자동 요청으로 큐에 넣음`() {
        /** given */
        val tower = readyTower(7L)
        whenever(towerAccessor.findCrawlDueTowersForUpdate(now, OciQueueClient.MAX_BATCH_SIZE))
            .thenReturn(listOf(tower))

        /** when */
        crawlRequestProducerService.requestDueCrawls()

        /** then */
        assertEquals(TowerStatus.CRAWLING, tower.status)
        verify(ociQueueClient).sendCrawlRequests(listOf(CrawlRequestDto.from(tower, now, null)))
    }

    @Test
    fun `수집 주기가 지난 타워가 없으면 큐에 넣지 않음`() {
        /** given */
        whenever(towerAccessor.findCrawlDueTowersForUpdate(now, OciQueueClient.MAX_BATCH_SIZE))
            .thenReturn(emptyList())

        /** when */
        crawlRequestProducerService.requestDueCrawls()

        /** then */
        verifyNoInteractions(ociQueueClient)
    }

    @Test
    fun `한 묶음을 가득 채워 요청하면 남은 타워를 다음 묶음으로 이어서 요청함`() {
        /** given */
        val fullBatch = (1L..OciQueueClient.MAX_BATCH_SIZE.toLong()).map { readyTower(it) }
        val remainingBatch = listOf(readyTower(100L))
        whenever(towerAccessor.findCrawlDueTowersForUpdate(now, OciQueueClient.MAX_BATCH_SIZE))
            .thenReturn(fullBatch, remainingBatch)

        /** when */
        crawlRequestProducerService.requestDueCrawls()

        /** then */
        verify(towerAccessor, times(2)).findCrawlDueTowersForUpdate(now, OciQueueClient.MAX_BATCH_SIZE)
        verify(ociQueueClient).sendCrawlRequests(fullBatch.map { CrawlRequestDto.from(it, now, null) })
        verify(ociQueueClient).sendCrawlRequests(remainingBatch.map { CrawlRequestDto.from(it, now, null) })
        verify(transactionManager, times(2)).commit(any())
    }

    @Test
    fun `큐에 넣지 못하면 그 묶음의 트랜잭션을 롤백하고 이후 묶음은 요청하지 않음`() {
        /** given */
        val tower = readyTower(7L)
        whenever(towerAccessor.findCrawlDueTowersForUpdate(now, OciQueueClient.MAX_BATCH_SIZE))
            .thenReturn(listOf(tower))
        whenever(ociQueueClient.sendCrawlRequests(any()))
            .thenThrow(BusinessException(CommonError.EXTERNAL_API_ERROR))

        /** when */
        assertFailsWith<BusinessException> {
            crawlRequestProducerService.requestDueCrawls()
        }

        /** then */
        verify(transactionManager).rollback(any())
        verify(transactionManager, never()).commit(any())
        verify(towerAccessor, times(1)).findCrawlDueTowersForUpdate(now, OciQueueClient.MAX_BATCH_SIZE)
    }
}
