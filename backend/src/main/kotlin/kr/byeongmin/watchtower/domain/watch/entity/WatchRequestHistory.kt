package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.entity.HistoryBase
import java.time.LocalDateTime

@Entity
class WatchRequestHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    private val watch: Watch,

    // 관리자가 트리거 하면 주인이 아니어도 트리거 가능
    // null 은 자동 트리거
    @ManyToOne(fetch = FetchType.LAZY)
    private val triggeredBy: Member? = null,

    private val url: String,
    private val startedAt: LocalDateTime,
    private val endedAt: LocalDateTime,
    private val httpStatusCode: Int
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