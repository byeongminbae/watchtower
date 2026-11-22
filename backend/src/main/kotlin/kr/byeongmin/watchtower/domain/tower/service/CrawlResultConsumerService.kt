package kr.byeongmin.watchtower.domain.tower.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerRequestHistoryAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.CrawlQueueClient
import kr.byeongmin.watchtower.domain.tower.client.SnapshotStorageClient
import kr.byeongmin.watchtower.domain.tower.dto.CrawlResultMessageDto
import kr.byeongmin.watchtower.domain.tower.dto.CrawlResultMessageDto.PageContent
import kr.byeongmin.watchtower.domain.tower.dto.QueueResultMessageDto
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerRequestHistory
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.domain.tower.sender.TowerNotificationSender
import kr.byeongmin.watchtower.global.alert.AdminNotifier
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import kr.byeongmin.watchtower.global.utils.isNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.concurrent.TimeUnit

/** 크롤러가 결과 큐에 보낸 크롤링 결과를 타워에 반영. 큐 기능을 끈 환경에서는 결과 큐를 받지 않음 */
@Service
@ConditionalOnProperty(name = [CrawlQueueClient.ENABLED_PROPERTY], havingValue = "true", matchIfMissing = true)
class CrawlResultConsumerService(
    private val crawlQueueClient: CrawlQueueClient,
    private val towerAccessor: TowerAccessor,
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val towerRequestHistoryAccessor: TowerRequestHistoryAccessor,
    private val memberAccessor: MemberAccessor,
    private val towerNotificationSender: TowerNotificationSender,
    private val snapshotStorageClient: SnapshotStorageClient,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
    private val adminNotifier: AdminNotifier,
    private val timeProvider: TimeProvider
) {
    private val logger = KotlinLogging.logger {}

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    fun applyCrawlResults() {
        val appliedResults = crawlQueueClient.fetchCrawlResults().filter { tryApplyInOwnTransaction(it) }
        if (appliedResults.isNotEmpty()) {
            crawlQueueClient.deleteCrawlResults(appliedResults.map { it.receipt })
        }
    }

    private fun tryApplyInOwnTransaction(queueResultMessage: QueueResultMessageDto): Boolean {
        try {
            val crawlResult = objectMapper.readValue<CrawlResultMessageDto>(queueResultMessage.content)
            transactionTemplate.executeWithoutResult { applyCrawlResult(crawlResult) }
            if (crawlResult.hasUnknownResultCode()) {
                adminNotifier.sendWarningAlert("크롤러와 백엔드간의 enum 불일치 발생 content=${queueResultMessage.content}")
            }
            return true
        } catch (exception: Exception) {
            val message = "크롤링 결과 반영 실패 content=${queueResultMessage.content}"
            logger.error(exception) { message }
            adminNotifier.sendErrorAlert(message, exception)
            return false
        }
    }

    private fun applyCrawlResult(crawlResult: CrawlResultMessageDto) {
        if (towerRequestHistoryAccessor.isCrawlResultApplied(crawlResult.towerId, crawlResult.crawlingStartedAt)) {
            return
        }
        val tower = towerAccessor.findTower(crawlResult.towerId) ?: return

        tower.recordCrawlResult(
            crawlingEndedAt = crawlResult.crawlingEndedAt,
            crawlerResultCode = crawlResult.crawlerResultCode,
            isCrawlingSucceed = crawlResult.isCrawlingSucceed()
        )
        val requestHistory = saveRequestHistory(tower, crawlResult)
        if (crawlResult.isCrawlingSucceed()) {
            saveSnapshotIfChanged(requestHistory, crawlResult.pageContent.ifNullThrow())
        }
    }

    private fun saveRequestHistory(tower: Tower, crawlResult: CrawlResultMessageDto): TowerRequestHistory {
        val requestHistory = TowerRequestHistory.of(
            tower = tower,
            requestedUrl = crawlResult.requestedUrl,
            finalUrl = crawlResult.finalUrl,
            jobStartedAt = crawlResult.jobStartedAt,
            jobEndedAt = timeProvider.now(),
            crawlingStartedAt = crawlResult.crawlingStartedAt,
            crawlingEndedAt = crawlResult.crawlingEndedAt,
            triggeredBy = crawlResult.triggeredByMemberId?.let { memberAccessor.findMember(it) },
            crawlerResultCode = crawlResult.crawlerResultCode,
            pageHttpStatusCode = crawlResult.pageHttpStatusCode
        )
        towerRequestHistoryAccessor.save(requestHistory)
        return requestHistory
    }

    private fun saveSnapshotIfChanged(requestHistory: TowerRequestHistory, pageContent: PageContent) {
        val textObjectName = pageContent.textObjectName ?: return
        val tower = requestHistory.tower
        val latestSnapshot = towerSnapshotAccessor.findLatestSnapshot(tower.id.ifNullThrow())
        if (latestSnapshot.isNull()) {
            saveSnapshotAndNotify(requestHistory, pageContent, textObjectName, isChangeDetected = false)
            return
        }

        val previous = latestSnapshot.to(tower.ignoreDateTime) {
            snapshotStorageClient.getText(latestSnapshot.textObjectName)
        }
        val current = pageContent.to(tower.ignoreDateTime) {
            snapshotStorageClient.getText(textObjectName)
        }
        if (previous.hasSameContent(current)) {
            // 크롤링 도중 날짜무시를 켜면 여기에 걸릴 수 있음
            snapshotStorageClient.deleteObject(textObjectName)
            return
        }

        saveSnapshotAndNotify(
            requestHistory = requestHistory,
            pageContent = pageContent,
            textObjectName = textObjectName,
            isChangeDetected = tower.detectsChange(previous = previous, current = current)
        )
    }

    private fun saveSnapshotAndNotify(
        requestHistory: TowerRequestHistory,
        pageContent: PageContent,
        textObjectName: String,
        isChangeDetected: Boolean
    ) {
        val notification = if (isChangeDetected) {
            towerNotificationSender.sendChangeDetected(requestHistory.tower)
        } else {
            null
        }
        val towerSnapshot = TowerSnapshot.of(
            towerRequestHistory = requestHistory,
            textObjectName = textObjectName,
            textHash = pageContent.textHash,
            dateTimeIgnoredTextHash = pageContent.dateTimeIgnoredTextHash,
            detectionAreaSelector = pageContent.detectionAreaSelector,
            imageHash = pageContent.imageHash,
            isChangeDetected = isChangeDetected,
            notification = notification
        )
        towerSnapshotAccessor.save(towerSnapshot)
    }
}
