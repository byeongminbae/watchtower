package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.external.oci.queue.client.OciQueueClient
import kr.byeongmin.watchtower.external.oci.queue.dto.CrawlRequestDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** 수집 주기가 지난 타워의 크롤링을 요청 큐에 넣음. 큐 기능을 끈 환경에서는 요청하지 않음 */
@Service
@ConditionalOnProperty(name = [OciQueueClient.QUEUE_ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class CrawlRequestProducerService(
    private val ociQueueClient: OciQueueClient,
    private val towerAccessor: TowerAccessor,
    private val transactionTemplate: TransactionTemplate,
    private val timeProvider: TimeProvider
) {
    /**
     * 수집 주기가 지난 타워를 [OciQueueClient.MAX_BATCH_SIZE] 개씩 크롤링 중으로 바꾸고 요청 큐에 넣음. 남은 타워가 없을 때까지 반복함
     *
     * 묶음마다 트랜잭션을 따로 커밋함. 큐에 넣지 못한 묶음은 롤백되어 크롤링 대기로 남고 다음 실행에서 다시 요청됨
     *
     * 이전 실행이 끝나고 1분 뒤에 다시 실행함
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun requestDueCrawls() {
        val now = timeProvider.now()
        do {
            val requestedCount = transactionTemplate.execute { requestDueCrawlBatch(now) }
        } while (requestedCount == OciQueueClient.MAX_BATCH_SIZE)
    }

    private fun requestDueCrawlBatch(now: LocalDateTime): Int {
        val towers = towerAccessor.findCrawlDueTowersForUpdate(now = now, limit = OciQueueClient.MAX_BATCH_SIZE)
        if (towers.isEmpty()) {
            return 0
        }

        towers.forEach { it.startCrawl() }
        ociQueueClient.sendCrawlRequests(towers.map { CrawlRequestDto.from(it, now, null) })
        return towers.size
    }
}
