package kr.byeongmin.watchtower.domain.tower.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerRequestHistoryAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.entity.TowerRequestHistory
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.external.oci.queue.client.OciQueueClient
import kr.byeongmin.watchtower.external.oci.queue.dto.CrawlResultDto
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.concurrent.TimeUnit

/** 크롤러가 결과 큐에 보낸 크롤링 결과를 타워에 반영. 큐 기능을 끈 환경에서는 결과 큐를 받지 않음 */
@Service
@ConditionalOnProperty(name = [OciQueueClient.QUEUE_ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class TowerCrawlResultService(
    private val ociQueueClient: OciQueueClient,
    private val towerAccessor: TowerAccessor,
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val towerRequestHistoryAccessor: TowerRequestHistoryAccessor,
    private val memberAccessor: MemberAccessor,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 결과 큐를 long polling 으로 받아 반영하고, 반영한 결과만 큐에서 지움
     *
     * 반영하지 못한 결과는 지우지 않아서 다시 전달되고, 전달 횟수가 큐의 한도를 넘으면 dead letter queue 로 옮겨짐
     *
     * 이전 실행이 끝나고 5초 뒤에 다시 받음. 큐 장애로 받기가 바로 실패할 때 요청이 1초마다 쌓이지 않게 하기 위함
     */
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    fun applyCrawlResults() {
        val appliedReceipts = mutableListOf<String>()

        ociQueueClient.fetchCrawlResults().forEach { queueResultMessageDto ->
            /**
             * 결과마다 트랜잭션을 따로 커밋함. 한 결과가 실패해도 나머지 결과는 반영하고 지울 수 있게 하기 위함
             *
             * 큐는 같은 결과를 다시 전달할 수 있어서, 다시 반영해도 상태가 바뀌지 않아야 함
             */
            try {
                val crawlResultDto = objectMapper.readValue<CrawlResultDto>(queueResultMessageDto.content)
                transactionTemplate.execute { applyCrawlResult(crawlResultDto) }
                appliedReceipts.add(queueResultMessageDto.receipt)
            } catch (exception: Exception) {
                logger.error(exception) { "크롤링 결과 반영 실패. content=${queueResultMessageDto.content}" }
            }
        }

        if (appliedReceipts.isNotEmpty()) {
            ociQueueClient.deleteCrawlResults(appliedReceipts)
        }
    }

    private fun applyCrawlResult(crawlResultDto: CrawlResultDto) {
        // 삭제된 타워의 결과는 반영하지 않고 버림
        val tower = towerAccessor.findTower(crawlResultDto.towerId) ?: return

        // 이력이 있으면 큐가 같은 결과를 다시 전달한 것이라 이미 반영했음
        if (towerRequestHistoryAccessor.existsByTowerIdAndStartedAt(tower.id.ifNullThrow(), crawlResultDto.crawlStartedAt)) {
            return
        }

        tower.applyCrawlResult(crawlResultDto)
        towerRequestHistoryAccessor.save(
            TowerRequestHistory.from(
                tower = tower,
                crawlResultDto = crawlResultDto,
                // TODO: 이전 수집본과 비교하는 변경 감지 구현 후 결과를 넣어야 함
                changed = false,
                triggeredBy = crawlResultDto.triggeredByMemberId?.let { memberAccessor.findMember(it) }
            )
        )

        // 실패한 크롤링은 저장한 HTML 이 없음
        val htmlObjectName = crawlResultDto.htmlObjectName ?: return
        towerSnapshotAccessor.save(TowerSnapshot.from(tower = tower, htmlContentUrl = htmlObjectName))
    }
}
