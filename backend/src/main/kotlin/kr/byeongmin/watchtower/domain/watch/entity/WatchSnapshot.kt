package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

@Entity
class WatchSnapshot(
    @ManyToOne(fetch = FetchType.LAZY)
    val watch: Watch,
    val htmlContentUrl: String,
    val htmlContentDiffUrl: String,
    val screenshotUrl: String,
    val screenshotDiffUrl: String,
    val aiDiffSummary: String,
    val notifiedAt: LocalDateTime
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "watch_snapshot_seq"
    )
    @SequenceGenerator(
        name = "watch_snapshot_seq",
        sequenceName = "watch_snapshot_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}