package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.external.oci.queue.dto.CrawlResultDto
import kr.byeongmin.watchtower.global.entity.HistoryBase
import java.time.LocalDateTime

/**
 * 타워를 확인(크롤링)한 요청의 이력
 * @property [tower] 해당 요청을 실행한 타워
 * @property [url] 요청 시점의 감시 대상 URL
 * @property [startedAt] 확인 요청을 시작한 시각
 * @property [endedAt] 확인 요청을 마친 시각
 * @property [httpStatusCode] 확인 응답의 HTTP 상태 코드. 응답을 받지 못하고 실패했으면 null
 * @property [errorCode] 크롤링에 실패한 이유(크롤러의 CrawlError 코드). 페이지를 받아왔으면 null
 * @property [triggeredBy] 요청을 시작한 회원. null은 자동 요청
 */
@Entity
class TowerRequestHistory private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val tower: Tower,
    val url: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val httpStatusCode: Int?,
    val errorCode: String?,
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
        fun from(
            tower: Tower,
            crawlResultDto: CrawlResultDto,
            triggeredBy: Member?
        ): TowerRequestHistory {
            return TowerRequestHistory(
                tower = tower,
                url = crawlResultDto.url,
                startedAt = crawlResultDto.crawlStartedAt,
                endedAt = crawlResultDto.crawlEndedAt,
                httpStatusCode = crawlResultDto.httpStatusCode,
                errorCode = crawlResultDto.errorCode,
                triggeredBy = triggeredBy
            )
        }
    }
}
