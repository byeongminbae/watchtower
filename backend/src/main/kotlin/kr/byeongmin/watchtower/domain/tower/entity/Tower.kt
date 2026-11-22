package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.domain.tower.vo.CssSelector
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Duration
import java.time.LocalDateTime

@Entity
@DynamicUpdate
class Tower private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val createdBy: Member,
    val url: String,
    var name: String,
    status: TowerStatus,
    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    var fetchInterval: Duration,
    var includeInStat: Boolean,
    var detectTextChange: Boolean,
    var detectImageChange: Boolean,
    var ignoreDateTime: Boolean,
    detectionAreaSelector: CssSelector?,
    var faviconUrl: String? = null,
    var lastNotifiedAt: LocalDateTime? = null
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "tower_seq"
    )
    @SequenceGenerator(
        name = "tower_seq",
        sequenceName = "tower_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    var detectionAreaSelector: String? = detectionAreaSelector?.value
        protected set

    @Enumerated(EnumType.STRING)
    var status: TowerStatus = status
        protected set

    var lastFetchedAt: LocalDateTime? = null
        protected set

    @Enumerated(EnumType.STRING)
    var lastCrawlerResultCode: CrawlerResultCode? = null
        protected set

    @OneToMany(mappedBy = "tower", fetch = FetchType.LAZY)
    val snapshots: MutableList<TowerSnapshot> = mutableListOf()

    @OneToMany(mappedBy = "tower", fetch = FetchType.LAZY)
    val conditions: MutableList<DetectionCondition> = mutableListOf()

    override fun delete() {
        super.delete()
        snapshots.forEach {
            if (!it.isDeleted()) {
                it.delete()
            }
        }
        conditions.forEach {
            if (!it.isDeleted()) {
                it.delete()
            }
        }
    }

    /** 엔티티 연관관계 편의 메서드에서만 사용해야함 */
    internal fun addSnapshot(snapshot: TowerSnapshot) {
        snapshots.add(snapshot)
    }

    /** 엔티티 연관관계 편의 메서드에서만 사용해야함 */
    internal fun addCondition(condition: DetectionCondition) {
        conditions.add(condition)
    }

    fun throwIfNothingToDetect() {
        if (!detectTextChange && !detectImageChange && conditions.all { it.isDeleted() }) {
            throw BusinessException(TowerError.HAS_NO_DETECTION)
        }
    }

    fun detectsChange(previous: DetectionContent, current: DetectionContent): Boolean {
        if (!previous.hasSameDetectionArea(current)) {
            /** 선택한 감시 영역을 바꾼 뒤의 첫 비교는 서로 다른 범위를 잰 내용이라 판정하지 않음
             * TODO: 이건 기준점 자체가 바뀐거라 이 짧은 순간에 바뀐 내용 변화는 감지못함
             *  감시 영역을 바꾸면 초기에 기존 영역과 겹치는 부분만 감지를 한다던지? 대책이 필요할듯 */
            return false
        }
        if (detectTextChange && previous.hasDifferentText(current)) {
            return true
        }
        if (detectImageChange && previous.hasDifferentImage(current)) {
            return true
        }
        return conditions.any { !it.isDeleted() && it.isConditionSatisfied(previous = previous, current = current) }
    }

    fun recordCrawlResult(crawlingEndedAt: LocalDateTime, crawlerResultCode: CrawlerResultCode, isCrawlingSucceed: Boolean) {
        this.lastFetchedAt = crawlingEndedAt
        this.lastCrawlerResultCode = crawlerResultCode
        if (status != TowerStatus.RUNNING) {
            return // 크롤링 중에 정지된 타워는 정지 상태를 유지하고 수집 시각만 기록함
        }

        this.status = if (isCrawlingSucceed) {
            TowerStatus.READY
        } else {
            TowerStatus.ERROR
        }
    }

    // TODO: 요청이 DLQ 로 가거나 만료돼 결과가 오지 않으면 RUNNING 에서 벗어나지 못함. 요청 시각을 기록하고 오래 RUNNING 인 타워를 ERROR 로 돌리는 스케줄러 추가
    fun markCrawlRequested() {
        if (status != TowerStatus.READY) {
            throw BusinessException(TowerError.CRAWL_NOT_REQUESTABLE)
        }
        this.status = TowerStatus.RUNNING
    }

    fun markCrawlRetryRequested() {
        if (status != TowerStatus.ERROR) {
            throw BusinessException(TowerError.CRAWL_NOT_RETRYABLE)
        }
        this.status = TowerStatus.RUNNING
    }

    fun changeStatusByAdmin(status: TowerStatus) {
        if (!status.isAdminChangeable()) {
            throw BusinessException(CommonError.INVALID_INPUT_VALUE)
        }
        this.status = status
    }

    fun throwIfNotMemberChangeable() {
        if (!status.isMemberChangeable()) {
            throw BusinessException(CommonError.PERMISSION_DENIED)
        }
    }

    fun pause() {
        if (!status.isPausable()) {
            throw BusinessException(TowerError.NOT_PAUSABLE)
        }
        this.status = TowerStatus.PAUSED
    }

    fun resume() {
        if (!status.isResumable()) {
            throw BusinessException(TowerError.NOT_RESUMABLE)
        }
        this.status = TowerStatus.READY
    }

    fun throwIfConditionQuotaExceeded() {
        if (conditions.count { !it.isDeleted() } > createdBy.plan.maxConditionQuantity) {
            throw BusinessException(TowerError.CONDITION_QUOTA_EXCEEDED)
        }
    }

    fun changeDetectionAreaSelector(detectionAreaSelector: CssSelector?) {
        this.detectionAreaSelector = detectionAreaSelector?.value
    }

    companion object {
        /** 소유자의 등록 한도는 별도로 검사해야 함 */
        fun of(
            createdBy: Member,
            name: String,
            url: TowerUrl,
            fetchInterval: Duration,
            includeInStat: Boolean,
            detectTextChange: Boolean,
            detectImageChange: Boolean,
            ignoreDateTime: Boolean,
            detectionAreaSelector: CssSelector?
        ): Tower {
            return Tower(
                createdBy = createdBy,
                name = name,
                url = url.value,
                fetchInterval = fetchInterval,
                includeInStat = includeInStat,
                detectTextChange = detectTextChange,
                detectImageChange = detectImageChange,
                ignoreDateTime = ignoreDateTime,
                detectionAreaSelector = detectionAreaSelector,
                status = TowerStatus.RUNNING
            )
        }
    }
}
