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
import kr.byeongmin.watchtower.external.oci.storage.client.OciSnapshotStorageClient
import kr.byeongmin.watchtower.external.slack.client.SlackClient
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
class CrawlResultConsumerService(
    private val ociQueueClient: OciQueueClient,
    private val towerAccessor: TowerAccessor,
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val towerRequestHistoryAccessor: TowerRequestHistoryAccessor,
    private val memberAccessor: MemberAccessor,
    private val ociSnapshotStorageClient: OciSnapshotStorageClient,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
    private val slackClient: SlackClient
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
                val message = "크롤링 결과 반영 실패. content=${queueResultMessageDto.content}"
                logger.error(exception) { message }
                slackClient.sendErrorAlert(message, exception)
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

        val towerRequestHistory = TowerRequestHistory.from(
            tower = tower,
            crawlResultDto = crawlResultDto,
            triggeredBy = crawlResultDto.triggeredByMemberId?.let { memberAccessor.findMember(it) }
        )
        towerRequestHistoryAccessor.save(towerRequestHistory)

        // 오류 페이지가 직전 버전이 되면 다음 정상 페이지를 변경으로 판정하므로, 2xx 로 받아온 페이지만 버전으로 저장하고 비교함
        // 크롤러가 직전 버전과 같은 HTML 이라 저장을 생략한 결과는 비교할 내용이 없어서 버전을 만들지 않음
        if (crawlResultDto.isNewHtmlVersion()) {
            saveHtmlVersion(towerRequestHistory, crawlResultDto)
        }
    }

    /** 이번 HTML 을 직전 버전과 타워의 조건으로 비교한 결과와 함께 새 버전으로 저장함. 첫 버전은 비교 대상이 없어서 조건을 충족하지 않은 것으로 저장함 */
    private fun saveHtmlVersion(towerRequestHistory: TowerRequestHistory, crawlResultDto: CrawlResultDto) {
        val htmlObjectName = crawlResultDto.htmlObjectName.ifNullThrow()
        val tower = towerRequestHistory.tower
        val previousSnapshot = towerSnapshotAccessor.findLatestSnapshot(tower.id.ifNullThrow())
        val isConditionSatisfied = previousSnapshot != null && tower.isConditionSatisfied(
            previousHtml = ociSnapshotStorageClient.getHtml(previousSnapshot.htmlObjectName),
            currentHtml = ociSnapshotStorageClient.getHtml(htmlObjectName)
        )

        towerSnapshotAccessor.save(
            TowerSnapshot.from(
                towerRequestHistory = towerRequestHistory,
                htmlObjectName = htmlObjectName,
                htmlSha256 = crawlResultDto.htmlSha256.ifNullThrow(),
                isConditionSatisfied = isConditionSatisfied
            )
        )
    }
}
