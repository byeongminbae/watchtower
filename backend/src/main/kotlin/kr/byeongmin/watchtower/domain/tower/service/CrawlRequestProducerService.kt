package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.CrawlQueueClient
import kr.byeongmin.watchtower.domain.tower.dto.CrawlRequestMessageDto
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(name = [CrawlQueueClient.ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class CrawlRequestProducerService(
    private val crawlQueueClient: CrawlQueueClient,
    private val towerAccessor: TowerAccessor,
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val transactionTemplate: TransactionTemplate,
    private val timeProvider: TimeProvider
) {
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun requestScheduledCrawls() {
        val now = timeProvider.now()
        do {
            /** 묶음마다 트랜잭션을 나눠서, 큐에 넣지 못하면 그 묶음의 상태 변경만 되돌림 */
            val scheduledTowerCount = transactionTemplate.execute { requestScheduledCrawlBatch(now) }.ifNullThrow()
        } while (scheduledTowerCount == CrawlQueueClient.MAX_BATCH_SIZE)
    }

    private fun requestScheduledCrawlBatch(now: LocalDateTime): Int {
        val scheduledTowers =
            towerAccessor.findCrawlScheduledTowersForUpdate(now = now, limit = CrawlQueueClient.MAX_BATCH_SIZE)
        scheduledTowers.forEach { it.markCrawlRequested() }

        requestCrawls(scheduledTowers, triggeredBy = null)
        return scheduledTowers.size
    }

    fun requestCrawls(towers: List<Tower>, triggeredBy: Member?) {
        if (towers.isEmpty()) {
            return
        }

        val latestSnapshots = towerSnapshotAccessor.findLatestSnapshots(towers.map { it.id.ifNullThrow() })
        crawlQueueClient.sendCrawlRequests(
            towers.map {
                CrawlRequestMessageDto.from(
                    tower = it,
                    latestSnapshot = latestSnapshots[it.id],
                    triggeredBy = triggeredBy,
                    jobStartedAt = timeProvider.now()
                )
            }
        )
    }
}
