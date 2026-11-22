package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.accessor.DetectionConditionAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.client.CrawlQueueClient
import kr.byeongmin.watchtower.domain.tower.client.SnapshotStorageClient
import kr.byeongmin.watchtower.domain.tower.dto.*
import kr.byeongmin.watchtower.domain.tower.entity.KeywordCondition
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.domain.tower.vo.CssSelector
import kr.byeongmin.watchtower.domain.tower.vo.Keyword
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.dns.DnsResolver
import kr.byeongmin.watchtower.external.google.GoogleFaviconClient
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 특정 사이트 공격이 될수도 있으니 요청은 스케줄러에게 맡기고 유저는 스케줄러에 만족하도록 상태만 바꿔주도록 설계해야함
 */
@Service
class TowerService(
    private val towerAccessor: TowerAccessor,
    private val detectionConditionAccessor: DetectionConditionAccessor,
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val memberAccessor: MemberAccessor,
    private val googleFaviconClient: GoogleFaviconClient,
    private val dnsResolver: DnsResolver,
    /** 큐 기능을 끈 환경([CrawlQueueClient.ENABLED_PROPERTY])에서는 null 이고, 크롤링을 요청하는 기능은 거부함 */
    private val crawlRequestProducerService: CrawlRequestProducerService?,
    /** 스토리지 기능을 끈 환경([SnapshotStorageClient.ENABLED_PROPERTY])에서는 null 이고, 스냅샷 파일 URL 발급을 거부함 */
    private val snapshotStorageClient: SnapshotStorageClient?,
    private val timeProvider: TimeProvider
) {
    /**
     * TODO: 등록하자마자 크롤링 요청을 보내서 등록하고 삭제하고 반복하면 임의 사이트 공격이 가능함
     *  정책을 세워야함
     */
    @Transactional
    fun createTower(memberId: Long, towerCreateRequestDto: TowerCreateRequestDto): SuccessResponse {
        /** 외부 크롤러는 이 검사에 절대 의지하지 말고 매번 요청마다 검사->요청 단계를 밟아야함 */
        validateIsExternalIp(towerCreateRequestDto.url)

        /** 동시성 이슈로 한도가 초과될수도 있어서 락을 검 */
        val member = memberAccessor.getMemberForUpdate(memberId)

        val newTower = towerCreateRequestDto.to(member)

        val detectionConditions = towerCreateRequestDto.conditions.map { it.to(newTower) }
        newTower.throwIfNothingToDetect()
        checkIsTowerQuotaExceeded(memberId, member)
        newTower.throwIfConditionQuotaExceeded()

        googleFaviconClient.buildFaviconUrl(newTower.url)?.let {
            newTower.faviconUrl = it
        }

        val savedTower = towerAccessor.saveTower(
            tower = newTower,
            conditions = detectionConditions
        )

        // 첫 크롤링 요청을 큐에 넣지 못하면 예외로 트랜잭션을 롤백해 타워 등록도 취소함
        sendCrawlRequest(savedTower, member)

        return SuccessResponse()
    }

    private fun sendCrawlRequest(tower: Tower, triggeredBy: Member) {
        val producer = crawlRequestProducerService ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
        producer.requestCrawls(listOf(tower), triggeredBy)
    }

    private fun validateIsExternalIp(towerUrl: TowerUrl) {
        towerUrl.validateIsExternalIp(dnsResolver)
    }

    private fun checkIsTowerQuotaExceeded(memberId: Long, member: Member) {
        val creatorTowerCount = towerAccessor.countOwnedTowers(memberId)
        if (!member.isTowerAddable(creatorTowerCount)) {
            throw BusinessException(CommonError.RESOURCE_QUOTA_EXCEEDED)
        }
    }

    @Transactional(readOnly = true)
    fun getTower(memberId: Long, towerId: Long): SuccessDataResponse<TowerResponseDto> {
        val existTower = towerAccessor.getOwnedTower(towerId = towerId, memberId = memberId)

        return SuccessDataResponse(toTowerResponse(existTower))
    }

    @Transactional
    fun updateTower(
        memberId: Long,
        towerId: Long,
        towerUpdateRequestDto: TowerUpdateRequestDto
    ): SuccessDataResponse<TowerResponseDto> {
        val existTower = towerAccessor.getOwnedTowerForUpdate(towerId = towerId, memberId = memberId)

        existTower.throwIfNotMemberChangeable()
        existTower.name = towerUpdateRequestDto.name
        existTower.fetchInterval = towerUpdateRequestDto.fetchInterval
        existTower.includeInStat = towerUpdateRequestDto.includeInStat
        existTower.detectTextChange = towerUpdateRequestDto.detectTextChange
        existTower.detectImageChange = towerUpdateRequestDto.detectImageChange
        existTower.ignoreDateTime = towerUpdateRequestDto.ignoreDateTime
        existTower.changeDetectionAreaSelector(towerUpdateRequestDto.detectionAreaSelector?.let { CssSelector(it) })
        existTower.throwIfNothingToDetect()

        return SuccessDataResponse(toTowerResponse(existTower))
    }

    private fun toTowerResponse(tower: Tower): TowerResponseDto {
        val latestAiSummary = towerSnapshotAccessor.findLatestChangeAiSummary(tower.id.ifNullThrow())
        return TowerResponseDto.from(tower, latestAiSummary)
    }

    @Transactional
    fun deleteTower(memberId: Long, towerId: Long): SuccessResponse {
        val existTower = towerAccessor.getOwnedTower(towerId = towerId, memberId = memberId)
        existTower.delete()
        // TODO: S3 객체 삭제 필요
        return SuccessResponse()
    }

    /** 같은 타워를 동시에 재시도해 크롤링 요청이 두 번 들어가지 않도록 락을 검. 큐에 넣지 못하면 롤백되어 실패 상태로 남음 */
    @Transactional
    fun retryCrawl(memberId: Long, towerId: Long): SuccessDataResponse<TowerResponseDto> {
        val existTower = towerAccessor.getOwnedTowerForUpdate(towerId = towerId, memberId = memberId)

        existTower.markCrawlRetryRequested()
        sendCrawlRequest(existTower, existTower.createdBy)

        return SuccessDataResponse(toTowerResponse(existTower))
    }

    @Transactional
    fun pauseTower(memberId: Long, towerId: Long): SuccessDataResponse<TowerResponseDto> {
        val existTower = towerAccessor.getOwnedTowerForUpdate(towerId = towerId, memberId = memberId)

        existTower.pause()

        return SuccessDataResponse(toTowerResponse(existTower))
    }

    @Transactional
    fun resumeTower(memberId: Long, towerId: Long): SuccessDataResponse<TowerResponseDto> {
        val existTower = towerAccessor.getOwnedTowerForUpdate(towerId = towerId, memberId = memberId)

        existTower.resume()

        return SuccessDataResponse(toTowerResponse(existTower))
    }

    @Transactional(readOnly = true)
    fun getDetectionConditions(
        memberId: Long,
        towerId: Long
    ): SuccessDataResponse<List<DetectionConditionResponseDto>> {
        val existCondition = detectionConditionAccessor.getOwnedDetectionConditions(
            towerId = towerId,
            memberId = memberId
        )

        return SuccessDataResponse(existCondition.map { DetectionConditionResponseDto.from(it) })
    }

    @Transactional
    fun createDetectionCondition(
        memberId: Long,
        towerId: Long,
        detectionConditionCreateRequestDto: DetectionConditionCreateRequestDto
    ): SuccessResponse {
        val existTower = towerAccessor.getOwnedTowerForUpdate(towerId = towerId, memberId = memberId)

        val condition = detectionConditionCreateRequestDto.to(existTower)
        existTower.throwIfConditionQuotaExceeded()
        detectionConditionAccessor.save(condition)

        return SuccessResponse()
    }

    @Transactional
    fun updateDetectionCondition(
        memberId: Long,
        towerId: Long,
        conditionId: Long,
        detectionConditionUpdateRequestDto: DetectionConditionUpdateRequestDto
    ): SuccessDataResponse<DetectionConditionResponseDto> {
        val condition = detectionConditionAccessor.getOwnedDetectionCondition(
            conditionId = conditionId,
            towerId = towerId,
            memberId = memberId
        )

        // TODO: 개선안 생각해보자
        when (condition) {
            is KeywordCondition if detectionConditionUpdateRequestDto is KeywordConditionUpdateRequestDto -> {
                condition.changeKeyword(Keyword(detectionConditionUpdateRequestDto.keyword))
                condition.satisfiedWhen = detectionConditionUpdateRequestDto.satisfiedWhen
            }

            else -> throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
        }

        return SuccessDataResponse(DetectionConditionResponseDto.from(condition))
    }

    @Transactional
    fun deleteDetectionCondition(memberId: Long, towerId: Long, conditionId: Long): SuccessResponse {
        val condition = detectionConditionAccessor.getOwnedDetectionCondition(
            conditionId = conditionId,
            towerId = towerId,
            memberId = memberId
        )

        condition.delete()
        towerAccessor.getOwnedTower(towerId = towerId, memberId = memberId).throwIfNothingToDetect()

        return SuccessResponse()
    }

    @Transactional(readOnly = true)
    fun getTowerChangedSnapshots(memberId: Long, towerId: Long): SuccessDataResponse<List<TowerSnapshotResponseDto>> {
        val tower = towerAccessor.getOwnedTower(towerId, memberId)
        val snapshots = towerSnapshotAccessor.findChangedSnapshots(tower)

        val readUrlExpiresAt = timeProvider.now().plus(TowerPolicy.SNAPSHOT_READ_URL_VALIDITY)
        return SuccessDataResponse(
            snapshots.map {
                TowerSnapshotResponseDto.from(
                    towerSnapshot = it,
                    highlightedScreenshotUrl = it.highlightedScreenshotObjectName?.let { objectName ->
                        createSnapshotReadUrl(objectName, readUrlExpiresAt)
                    }
                )
            }
        )
    }

    private fun createSnapshotReadUrl(objectName: String, expiresAt: LocalDateTime): String {
        val storageClient = snapshotStorageClient ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
        return storageClient.createReadUrl(objectName, expiresAt)
    }
}
