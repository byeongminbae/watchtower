package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

/**
 * [Tower.url] 을 크롤링해 2xx 로 받아온 페이지의 버전. 같은 타워의 가장 최근 스냅샷이 다음 크롤링의 비교 대상이 됨
 *
 * 크롤러가 받아온 HTML 의 SHA-256 이 가장 최근 스냅샷의 [htmlSha256] 과 같으면 저장을 생략하므로, 첫 버전이 아닌 스냅샷은 모두 직전 버전과 HTML 이 다름
 *
 * @property [htmlObjectName] 크롤러가 HTML 을 저장한 버킷의 객체 이름
 * @property [htmlSha256] 저장한 HTML 의 SHA-256 (소문자 16진수). 다음 크롤링 요청에 직전 버전 해시로 담아 보냄
 * @property [highlightedScreenshotObjectName] 이번 스크린샷 위에 직전 버전과 달라진 영역을 표시한 이미지의 객체 이름. 첫 버전이거나 화면이 바뀌지 않았으면 null
 * @property [changed] 직전 버전과 비교해 타워의 조건 중 하나라도 충족했는지. 첫 버전은 비교 대상이 없어서 false
 */
@Entity
class TowerSnapshot private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val tower: Tower,
    val htmlObjectName: String,
    val htmlSha256: String,
    // TODO: 크롤러가 스크린샷을 저장하게 되면 NOT NULL 로 변경
    val screenshotObjectName: String?,
    val highlightedScreenshotObjectName: String?,
    val changed: Boolean,
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
            htmlObjectName: String,
            htmlSha256: String,
            changed: Boolean,
            screenshotObjectName: String? = null,
            highlightedScreenshotObjectName: String? = null,
            aiDiffSummary: String? = null,
            notifiedAt: LocalDateTime? = null
        ): TowerSnapshot {
            val snapshot = TowerSnapshot(
                tower = tower,
                htmlObjectName = htmlObjectName,
                htmlSha256 = htmlSha256,
                screenshotObjectName = screenshotObjectName,
                highlightedScreenshotObjectName = highlightedScreenshotObjectName,
                changed = changed,
                aiDiffSummary = aiDiffSummary,
                notifiedAt = notifiedAt
            )
            tower.addSnapshot(snapshot)

            return snapshot
        }
    }
}
