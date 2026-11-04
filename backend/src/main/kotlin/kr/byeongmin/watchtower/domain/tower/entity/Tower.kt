package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.domain.tower.enums.UpdatableTowerStatus
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.external.oci.queue.dto.CrawlResultDto
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Duration
import java.time.LocalDateTime

/**
 * @property [url] 변경을 허용하게 되면 기존 기록(특히 스냅샷)들과 매칭이 안되므로 불변으로 감
 * @property [fetchInterval] 페이지 수집 주기(초)
 * @property [includeInStat] 통계 포함 여부
 * @property [towerNotificationChannels] 변경 알림을 전송할 채널과의 연결 정보
 */
@Entity
@DynamicUpdate
class Tower private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val createdBy: Member,
    val url: String,
    name: String,
    fetchInterval: Duration,
    includeInStat: Boolean,
    status: TowerStatus
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

    var name: String = name
        protected set

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    var fetchInterval: Duration = fetchInterval
        protected set

    var includeInStat: Boolean = includeInStat
        protected set

    @Enumerated(EnumType.STRING)
    var status: TowerStatus = status
        protected set

    var faviconUrl: String? = null
        protected set

    var lastFetchedAt: LocalDateTime? = null
        protected set

    var lastNotifiedAt: LocalDateTime? = null
        protected set

    @OneToMany(mappedBy = "tower", fetch = FetchType.LAZY)
    val snapshots: MutableList<TowerSnapshot> = mutableListOf()

    @OneToMany(mappedBy = "tower", fetch = FetchType.LAZY)
    val conditions: MutableList<TowerCondition> = mutableListOf()

    @OneToMany(
        mappedBy = "tower",
        fetch = FetchType.LAZY
    )
    val towerNotificationChannels: MutableList<TowerNotificationChannel> = mutableListOf()

    /** 타워와 아직 삭제되지 않은 소속 스냅샷 및 조건을 함께 논리 삭제 */
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
        towerNotificationChannels.forEach {
            if (!it.isDeleted()) {
                it.delete()
            }
        }
    }

    fun changeFaviconUrl(faviconUrl: String) {
        this.faviconUrl = faviconUrl
    }

    /** 엔티티 연관관계 편의 메서드에서만 사용해야함 */
    fun addSnapshot(snapshot: TowerSnapshot) {
        snapshots.add(snapshot)
    }

    /** 엔티티 연관관계 편의 메서드에서만 사용해야함 */
    fun addCondition(condition: TowerCondition) {
        conditions.add(condition)
    }

    fun hasHtmlCondition(): Boolean {
        return conditions.any { it is HtmlCondition && !it.isDeleted() }
    }

    /** 삭제되지 않은 감지 조건 중 하나라도 직전 버전과 이번 버전의 HTML 비교에서 충족했는지 판정함. 조건이 없으면 false */
    fun isConditionSatisfied(previousHtml: String, currentHtml: String): Boolean {
        return conditions.any { !it.isDeleted() && it.isSatisfied(previousHtml = previousHtml, currentHtml = currentHtml) }
    }

    /**
     * 크롤링 결과를 반영함. 크롤링 중인 타워만 결과([CrawlResultDto.isPageReceived])에 따라 [TowerStatus.CRAWLING_READY] 나 [TowerStatus.CRAWLING_ERROR] 로 바뀜
     *
     * 크롤링 중에 정지된 타워는 정지 상태를 유지하고 수집 시각만 기록함
     */
    fun applyCrawlResult(crawlResultDto: CrawlResultDto) {
        this.lastFetchedAt = crawlResultDto.crawlEndedAt
        if (status != TowerStatus.CRAWLING) {
            return
        }

        this.status = if (crawlResultDto.isPageReceived()) TowerStatus.CRAWLING_READY else TowerStatus.CRAWLING_ERROR
    }

    /** 크롤링 대기 중인 타워를 크롤링 중으로 바꿈. 크롤링 요청은 호출하는 쪽에서 큐에 넣어야 함 */
    fun startCrawl() {
        check(status == TowerStatus.CRAWLING_READY) { "크롤링 대기 중인 타워만 크롤링을 시작할 수 있음. status=$status" }
        this.status = TowerStatus.CRAWLING
    }

    /** 크롤링에 실패한 타워를 다시 크롤링 중으로 바꿈. 크롤링 요청은 호출하는 쪽에서 큐에 넣어야 함 */
    fun retryCrawl() {
        if (status != TowerStatus.CRAWLING_ERROR) {
            throw BusinessException(TowerError.CRAWL_NOT_RETRYABLE)
        }
        this.status = TowerStatus.CRAWLING
    }

    fun notifiedAt(now: LocalDateTime) {
        this.lastNotifiedAt = now
    }

    /** 어드민 전용 강제 타워 상태 변경. [TowerStatus.CRAWLING] 은 크롤링 요청과 함께만 될 수 있어서 바꿔 넣을 수 없음 */
    fun changeStatusByAdmin(status: TowerStatus) {
        if (status == TowerStatus.CRAWLING) {
            throw BusinessException(CommonError.INVALID_INPUT_VALUE)
        }
        this.status = status
    }

    /**
     * 회원 전용 타워 상태 변경
     *
     * 크롤링 대기로 바꾸는 요청은 일시정지한 타워에만 적용되고, 크롤링 중이거나 실패한 타워는 지금 상태를 유지함
     * 크롤링 중인 타워를 대기로 바꾸면 들어올 결과가 상태에 반영되지 않고, 실패한 타워는 재시도로만 다시 크롤링하기 때문
     */
    fun changeStatusByMember(targetStatus: UpdatableTowerStatus) {
        if (!status.isMemberChangeable()) {
            throw BusinessException(CommonError.PERMISSION_DENIED)
        }
        when (targetStatus) {
            UpdatableTowerStatus.USER_PAUSED -> this.status = TowerStatus.USER_PAUSED
            UpdatableTowerStatus.CRAWLING_READY -> if (status == TowerStatus.USER_PAUSED) {
                this.status = TowerStatus.CRAWLING_READY
            }
        }
    }

    /** 현재 조건에 새 조건을 추가해도 이용 중인 플랜의 타워별 조건 한도를 넘지 않는지 검사 */
    fun isConditionAddable(currentQuantity: Long, additionalQuantity: Long): Boolean {
        return (currentQuantity + additionalQuantity) <= createdBy.plan.maxConditionQuantity
    }

    fun rename(name: String) {
        this.name = name
    }

    fun changeFetchInterval(fetchInterval: Duration) {
        this.fetchInterval = fetchInterval
    }

    fun changeStatInclusion(includeInStat: Boolean) {
        this.includeInStat = includeInStat
    }

    companion object {
        /** 소유자의 등록 한도는 별도로 검사해야 함. 등록과 함께 첫 크롤링을 요청하므로 크롤링 중 상태로 시작함 */
        fun from(createdBy: Member, towerCreateRequestDto: TowerCreateRequestDto): Tower {
            return Tower(
                createdBy = createdBy,
                name = towerCreateRequestDto.name,
                url = towerCreateRequestDto.url.value,
                fetchInterval = towerCreateRequestDto.fetchInterval,
                includeInStat = towerCreateRequestDto.includeInStat,
                status = TowerStatus.CRAWLING
            )
        }
    }
}
