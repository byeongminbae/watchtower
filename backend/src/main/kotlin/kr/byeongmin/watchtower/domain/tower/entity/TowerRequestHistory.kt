package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import kr.byeongmin.watchtower.global.entity.HistoryBase
import java.time.LocalDateTime

/**
 * 타워를 확인(크롤링)한 요청의 이력
 *
 * @property [requestedUrl] 크롤링을 요청한 URL(요청 시점의 감시 대상 URL). 리다이렉트된 최종 URL 이 아님
 * @property [jobStartedAt] 백엔드가 크롤링 작업을 시작한 시각(요청 큐에 넣은 시각)
 * @property [crawlingStartedAt] 크롤러가 크롤링을 시작한 시각
 * @property [crawlingEndedAt] 크롤러가 크롤링을 마친 시각
 * @property [triggeredBy] 요청을 시작한 회원. null은 자동 요청
 * @property [crawlerResultCode] 크롤러가 판정한 크롤링 결과. 4xx, 5xx 응답도 페이지를 받아온 것이라 [CrawlerResultCode.FETCHED], 실패하면 크롤러가 실패한 이유
 * @property [finalUrl] 리다이렉트를 모두 따라간 뒤 페이지를 응답한 URL. 추적 파라미터 등으로 길어질 수 있어 길이 제한이 없음. 페이지를 받아오지 못했으면 null
 * @property [pageHttpStatusCode] 감시 대상 페이지가 돌려준 HTTP 상태 코드. 응답을 받지 못하고 실패했으면 null
 * @property [jobEndedAt] 백엔드가 크롤링 결과를 받아 반영한 시각
 */
@Entity
class TowerRequestHistory private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val tower: Tower,
    val requestedUrl: String,
    val finalUrl: String?,
    val jobStartedAt: LocalDateTime,
    val jobEndedAt: LocalDateTime,
    val crawlingStartedAt: LocalDateTime,
    val crawlingEndedAt: LocalDateTime,
    @ManyToOne(fetch = FetchType.LAZY)
    val triggeredBy: Member?,
    @Enumerated(EnumType.STRING)
    val crawlerResultCode: CrawlerResultCode,
    val pageHttpStatusCode: Int?
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
        fun of(
            tower: Tower,
            requestedUrl: String,
            finalUrl: String?,
            jobStartedAt: LocalDateTime,
            jobEndedAt: LocalDateTime,
            crawlingStartedAt: LocalDateTime,
            crawlingEndedAt: LocalDateTime,
            triggeredBy: Member?,
            crawlerResultCode: CrawlerResultCode,
            pageHttpStatusCode: Int?
        ): TowerRequestHistory {
            return TowerRequestHistory(
                tower = tower,
                requestedUrl = requestedUrl,
                finalUrl = finalUrl,
                jobStartedAt = jobStartedAt,
                jobEndedAt = jobEndedAt,
                crawlingStartedAt = crawlingStartedAt,
                crawlingEndedAt = crawlingEndedAt,
                triggeredBy = triggeredBy,
                crawlerResultCode = crawlerResultCode,
                pageHttpStatusCode = pageHttpStatusCode
            )
        }
    }
}
