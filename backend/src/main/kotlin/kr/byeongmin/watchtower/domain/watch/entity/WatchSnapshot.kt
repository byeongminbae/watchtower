package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

@Entity
class WatchSnapshot(
    @ManyToOne(fetch = FetchType.LAZY)
    private val watch: Watch,
    private val htmlContentUrl: String,
    private val htmlContentDiffUrl: String,
    private val screenshotUrl: String,
    private val screenshotDiffUrl: String,
    private val aiDiffSummary: String,
    private val notifiedAt: LocalDateTime
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