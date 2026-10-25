package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationChannelAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerConditionAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.dto.*
import kr.byeongmin.watchtower.domain.tower.entity.*
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.dns.DnsResolver
import kr.byeongmin.watchtower.external.google.GoogleFaviconClient
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TowerService(
    private val towerAccessor: TowerAccessor,
    private val towerConditionAccessor: TowerConditionAccessor,
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val memberAccessor: MemberAccessor,
    private val notificationChannelAccessor: NotificationChannelAccessor,
    private val googleFaviconClient: GoogleFaviconClient,
    private val dnsResolver: DnsResolver
) {
    @Transactional
    fun createTower(memberId: Long, towerCreateRequestDto: TowerCreateRequestDto): SuccessResponse {
        /** 외부 크롤러는 이 검사에 절대 의지하지 말고 매번 요청마다 검사->요청 단계를 밟아야함 */
        validateIsExternalIp(towerCreateRequestDto.url)

        /** 동시성 이슈로 한도가 초과될수도 있어서 락을 검 */
        val member = memberAccessor.getMemberForUpdate(memberId)

        // TODO: 1차적으로 여기서 Playwright 로 URL 접근 가능 여부 검사 필요함(4xx 응답, 빈 페이지)

        val newTower = Tower.from(createdBy = member, towerCreateRequestDto = towerCreateRequestDto)

        val towerConditions = towerCreateRequestDto.conditions.map { it.to(newTower) }
        checkIsTowerQuotaExceeded(memberId, member)
        checkIsTowerConditionQuotaExceeded(newTower, towerConditions)

        googleFaviconClient.buildFaviconUrl(newTower.url)?.let {
            newTower.changeFaviconUrl(it)
        }

        // tower.save 전에 채널 아이디가 잘못된건 없는지 검사도됨
        val towerNotificationChannels = buildTowerNotificationChannels(towerCreateRequestDto, memberId, newTower)

        towerAccessor.saveTower(
            tower = newTower,
            conditions = towerConditions,
            notificationChannels = towerNotificationChannels
        )

        return SuccessResponse()
    }

    private fun validateIsExternalIp(towerUrl: TowerUrl) {
        towerUrl.validateIsExternalIp(dnsResolver)
    }

    private fun checkIsTowerConditionQuotaExceeded(
        tower: Tower,
        towerConditions: List<TowerCondition>
    ) {
        val isConditionAddable = tower.isConditionAddable(
            currentQuantity = 0,
            additionalQuantity = towerConditions.size.toLong()
        )
        if (!isConditionAddable) {
            throw BusinessException(TowerError.CONDITION_QUOTA_EXCEEDED)
        }
    }

    private fun checkIsTowerQuotaExceeded(memberId: Long, member: Member) {
        val creatorTowerCount = towerAccessor.countOwnedTowers(memberId)
        if (!member.isTowerAddable(creatorTowerCount)) {
            throw BusinessException(CommonError.RESOURCE_QUOTA_EXCEEDED)
        }
    }

    private fun buildTowerNotificationChannels(
        towerCreateRequestDto: TowerCreateRequestDto,
        memberId: Long,
        tower: Tower
    ): List<TowerNotificationChannel> {
        val notificationChannels = notificationChannelAccessor.findByIdsAndMemberId(
            channelIds = towerCreateRequestDto.notificationChannelIds,
            memberId = memberId
        )

        if (notificationChannels.size != towerCreateRequestDto.notificationChannelIds.size) {
            throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
        }

        val towerNotificationChannels = notificationChannels.map {
            TowerNotificationChannel.of(
                tower = tower,
                notificationChannel = it
            )
        }
        return towerNotificationChannels
    }

    @Transactional(readOnly = true)
    fun getTower(memberId: Long, towerId: Long): SuccessDataResponse<TowerResponseDto> {
        val existTower = towerAccessor.getOwnedTower(towerId = towerId, memberId = memberId)

        return SuccessDataResponse(TowerResponseDto.from(existTower))
    }

    @Transactional
    fun updateTower(
        memberId: Long,
        towerId: Long,
        towerUpdateRequestDto: TowerUpdateRequestDto
    ): SuccessDataResponse<TowerResponseDto> {
        val existTower = towerAccessor.getOwnedTower(towerId = towerId, memberId = memberId)

        existTower.changeStatusByMember(towerUpdateRequestDto.status)
        existTower.changeFetchInterval(towerUpdateRequestDto.fetchInterval)
        existTower.rename(towerUpdateRequestDto.name)
        existTower.changeStatInclusion(towerUpdateRequestDto.includeInStat)

        return SuccessDataResponse(TowerResponseDto.from(existTower))
    }

    @Transactional
    fun deleteTower(memberId: Long, towerId: Long): SuccessResponse {
        val existTower = towerAccessor.getOwnedTower(towerId = towerId, memberId = memberId)
        existTower.delete()
        // TODO: S3 객체 삭제 필요
        return SuccessResponse()
    }

    @Transactional(readOnly = true)
    fun sendTestNotification(memberId: Long, towerId: Long): SuccessResponse {
        towerAccessor.getOwnedTower(towerId = towerId, memberId = memberId)

        // TODO: 외부 알림 서비스(Discord/Telegram 전송) 연동 필요
        //  - 전송 대상은 회원의 NotificationChannel(DISCORD/TELEGRAM 종류별 webhookUrl)에서 조회
        //  - 알림 내용: 사이트 이름, URL, AI 요약, 변경 강조
        //  - 알림 실패 시 백오프+jitter 로 최대 10회 재시도 후 슬랙 알림(긴급)
        //  - 재시도 유실 방지를 위해 SQS 영속 필요

        return SuccessResponse()
    }

    @Transactional(readOnly = true)
    fun getTowerConditions(memberId: Long, towerId: Long): SuccessDataResponse<List<TowerConditionResponseDto>> {
        val existCondition = towerConditionAccessor.getOwnedTowerConditions(
            towerId = towerId,
            memberId = memberId
        )

        return SuccessDataResponse(existCondition.map { TowerConditionResponseDto.from(it) })
    }

    @Transactional
    fun createTowerCondition(
        memberId: Long,
        towerId: Long,
        towerConditionCreateRequestDto: TowerConditionCreateRequestDto
    ): SuccessResponse {
        val existTower = towerAccessor.getOwnedTowerForUpdate(towerId = towerId, memberId = memberId)

        val conditionCount = towerConditionAccessor.countByTower(existTower)
        if (!existTower.isConditionAddable(currentQuantity = conditionCount, additionalQuantity = 1)) {
            throw BusinessException(TowerError.CONDITION_QUOTA_EXCEEDED)
        }

        val condition = towerConditionCreateRequestDto.to(existTower)
        towerConditionAccessor.save(condition)

        return SuccessResponse()
    }

    @Transactional
    fun updateTowerCondition(
        memberId: Long,
        towerId: Long,
        conditionId: Long,
        towerConditionUpdateRequestDto: TowerConditionUpdateRequestDto
    ): SuccessDataResponse<TowerConditionResponseDto> {
        val condition = towerConditionAccessor.getOwnedCondition(
            conditionId = conditionId,
            towerId = towerId,
            memberId = memberId
        )

        when (condition) {
            is RegexCondition if towerConditionUpdateRequestDto is RegexConditionUpdateRequestDto -> {
                condition.changeRegex(towerConditionUpdateRequestDto.regex)
                condition.changeSatisfactionDirection(towerConditionUpdateRequestDto.isSatisfiedAtAppearance)
            }

            is KeywordCondition if towerConditionUpdateRequestDto is KeywordConditionUpdateRequestDto -> {
                condition.changeKeyword(towerConditionUpdateRequestDto.keyword)
                condition.changeSatisfactionDirection(towerConditionUpdateRequestDto.isSatisfiedAtAppearance)
            }

            else -> throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
        }

        return SuccessDataResponse(TowerConditionResponseDto.from(condition))
    }

    @Transactional
    fun deleteTowerCondition(memberId: Long, towerId: Long, conditionId: Long): SuccessResponse {
        val condition = towerConditionAccessor.getOwnedCondition(
            conditionId = conditionId,
            towerId = towerId,
            memberId = memberId
        )

        condition.delete()

        return SuccessResponse()
    }

    @Transactional(readOnly = true)
    fun getTowerSnapshots(memberId: Long, towerId: Long): SuccessDataResponse<List<TowerSnapshotResponseDto>> {
        val snapshots = towerSnapshotAccessor.getMemberSnapshots(
            towerId = towerId,
            memberId = memberId
        )

        return SuccessDataResponse(snapshots.map { TowerSnapshotResponseDto.from(it) })
    }
}
