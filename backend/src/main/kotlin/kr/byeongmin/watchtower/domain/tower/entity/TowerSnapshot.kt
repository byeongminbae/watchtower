package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

/**
 * [Tower.url] 을 크롤링 할 때마다 생성되는 엔티티
 *
 *  아래 nullable 필드들은 이전 수집본과 비교한 변경 결과라서, 첫 수집이거나 변경이 없으면 전부 null 이다
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
