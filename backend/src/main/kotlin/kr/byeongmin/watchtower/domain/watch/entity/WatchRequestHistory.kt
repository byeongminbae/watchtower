package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.HistoryBase
import java.time.LocalDateTime

@Entity
class WatchRequestHistory private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val watch: Watch,
    // 관리자가 트리거 하면 주인이 아니어도 트리거 가능
    // null 은 자동 트리거
    @ManyToOne(fetch = FetchType.LAZY)
    val triggeredBy: Member? = null,
    val url: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    val httpStatusCode: Int
) : HistoryBase() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "watch_request_history_seq"
    )
    @SequenceGenerator(
        name = "watch_request_history_seq",
        sequenceName = "watch_request_history_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}