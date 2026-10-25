package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.utils.isNull
import java.time.LocalDateTime

/**
 * [Tower.url] 을 크롤링 할 때마다 생성되는 엔티티
 *
 *  아래 nullable 필드들은 첫 크롤링에선 전부 null 이다.(head 로 취급)
 */
@Entity
class TowerSnapshot private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val tower: Tower,
    val htmlContentUrl: String,
    val htmlContentDiffUrl: String?,
    val screenshotUrl: String?,
    val screenshotDiffUrl: String?,
    val aiDiffSummary: String?,
    val notifiedAt: LocalDateTime?
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "tower_snapshot_seq"
    )
    @SequenceGenerator(
        name = "tower_snapshot_seq",
        sequenceName = "tower_snapshot_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    fun isHead(): Boolean {
        return htmlContentDiffUrl.isNull() &&
                screenshotUrl.isNull() &&
                screenshotDiffUrl.isNull() &&
                aiDiffSummary.isNull() &&
                notifiedAt.isNull()
    }

    companion object {
        fun from(
            tower: Tower,
            htmlContentUrl: String,
            htmlContentDiffUrl: String? = null,
            screenshotUrl: String? = null,
            screenshotDiffUrl: String? = null,
            aiDiffSummary: String? = null,
            notifiedAt: LocalDateTime? = null
        ): TowerSnapshot {
            val snapshot = TowerSnapshot(
                tower = tower,
                htmlContentUrl = htmlContentUrl,
                htmlContentDiffUrl = htmlContentDiffUrl,
                screenshotUrl = screenshotUrl,
                screenshotDiffUrl = screenshotDiffUrl,
                aiDiffSummary = aiDiffSummary,
                notifiedAt = notifiedAt
            )
            tower.addSnapshot(snapshot)

            return snapshot
        }
    }
}
