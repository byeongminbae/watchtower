package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import java.time.Duration
import java.time.LocalDateTime
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.dto.TowerCreateRequestDto
import kr.byeongmin.watchtower.domain.tower.enums.UpdatableTowerStatus
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

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

    fun fetchedAt(now: LocalDateTime) {
        this.lastFetchedAt = now
    }

    fun notifiedAt(now: LocalDateTime) {
        this.lastNotifiedAt = now
    }

    fun pause() {
        if (status == TowerStatus.RUNNING) {
            this.status = TowerStatus.PAUSED
        }
    }

    /** 어드민 전용 강제 타워 상태 변경 */
    fun changeStatusByAdmin(status: TowerStatus) {
        this.status = status
    }

    /** 회원 전용 타워 상태 변경 */
    fun changeStatusByMember(targetStatus: UpdatableTowerStatus) {
        if (!status.isMemberChangeable()) {
            throw BusinessException(CommonError.PERMISSION_DENIED)
        }
        status = targetStatus.towerStatus
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
        /** 소유자의 등록 한도는 별도로 검사해야 함 */
        fun from(createdBy: Member, towerCreateRequestDto: TowerCreateRequestDto): Tower {
            return Tower(
                createdBy = createdBy,
                name = towerCreateRequestDto.name,
                url = towerCreateRequestDto.url.value,
                fetchInterval = towerCreateRequestDto.fetchInterval,
                includeInStat = towerCreateRequestDto.includeInStat,
                status = TowerStatus.RUNNING
            )
        }
    }
}
