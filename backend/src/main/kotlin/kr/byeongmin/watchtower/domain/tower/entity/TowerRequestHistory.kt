package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.HistoryBase
import java.time.LocalDateTime

/**
 * 타워를 확인(크롤링)한 요청의 이력
 * @property [tower] 해당 요청을 실행한 타워
 * @property [url] 요청 시점의 감시 대상 URL
 * @property [startedAt] 확인 요청을 시작한 시각
 * @property [endedAt] 확인 요청을 마친 시각
 * @property [httpStatusCode] 확인 응답의 HTTP 상태 코드
 * @property [changed] 이번 요청에서 변화가 감지되었는지 여부 (Hot 타워리스트 집계 기준)
 * @property [triggeredBy] 요청을 시작한 회원. null은 자동 요청
 */
@Entity
class TowerRequestHistory private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val tower: Tower,
    val url: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val httpStatusCode: Int,
    val changed: Boolean,
    @ManyToOne(fetch = FetchType.LAZY)
    val triggeredBy: Member? = null
) : HistoryBase() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "tower_request_history_seq"
    )
    @SequenceGenerator(
        name = "tower_request_history_seq",
        sequenceName = "tower_request_history_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    companion object {
        /** 시간 순서나 HTTP 상태 코드 범위는 검증하지 않음 */
        fun from(
            tower: Tower,
            url: String,
            startedAt: LocalDateTime,
            endedAt: LocalDateTime,
            httpStatusCode: Int,
            changed: Boolean,
            triggeredBy: Member?
        ): TowerRequestHistory {
            return TowerRequestHistory(
                tower = tower,
                url = url,
                startedAt = startedAt,
                endedAt = endedAt,
                httpStatusCode = httpStatusCode,
                changed = changed,
                triggeredBy = triggeredBy
            )
        }
    }
}
