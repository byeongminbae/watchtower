package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

/**
 * [Tower.url] 을 크롤링해 2xx 로 받아온 페이지의 버전. 같은 타워의 가장 최근 스냅샷이 다음 크롤링의 비교 대상이 됨
 *
 * @property [towerRequestHistory] 이 버전을 받아온 크롤링 요청의 이력
 * @property [textObjectName] 스냅샷마다 크롤링 시작 시각으로 따로 저장한 객체라 다른 스냅샷과 공유하지 않음
 * @property [textHash] 저장한 텍스트 원문의 SHA-256 (소문자 16진수)
 * @property [dateTimeIgnoredTextHash] 저장한 텍스트에서 날짜와 시간 표기를 뺀 SHA-256. 날짜 무시 설정을 바꿔도 직전 버전과 같은 기준으로 비교할 수 있도록 설정과 상관없이 함께 저장함
 * @property [detectionAreaSelector] 이 버전을 뽑은 감시 영역의 선택자. 페이지 전체면 null. 영역이 다른 버전끼리는 비교하지 않음
 * @property [imageHash] 이미지를 수집하지 않은 버전이면 null
 * @property [highlightedScreenshotObjectName] 이번 스크린샷 위에 직전 버전과 달라진 영역을 표시한 이미지의 객체 이름. 첫 버전이거나 화면이 바뀌지 않았으면 null
 * @property [isChangeDetected] 직전 버전과 비교해 켜진 감지 옵션이나 삭제되지 않은 감지 조건 중 하나라도 충족했는지. 텍스트가 달라졌다는 뜻이 아님. 예를 들어 [KeywordCondition] 만 있는 타워는 텍스트가 달라져도 키워드 등장 횟수가 같으면 false. 첫 버전은 비교 대상이 없어서 false
 */
@Entity
class TowerSnapshot private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val tower: Tower,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    val towerRequestHistory: TowerRequestHistory,
    val textObjectName: String,
    val textHash: String,
    val dateTimeIgnoredTextHash: String,
    val detectionAreaSelector: String?,
    val imageHash: String?,
    // TODO: 크롤러가 스크린샷을 저장하게 되면 NOT NULL 로 변경
    val screenshotObjectName: String?,
    val highlightedScreenshotObjectName: String?,
    val isChangeDetected: Boolean,
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

    fun selectTextHash(ignoreDateTime: Boolean): String {
        if (ignoreDateTime) {
            return dateTimeIgnoredTextHash
        }
        return textHash
    }

    fun hasSameDetectionArea(detectionAreaSelector: String?): Boolean {
        return this.detectionAreaSelector == detectionAreaSelector
    }

    /** @param [textLoader] 텍스트가 필요한 조건이 읽을 때만 이 스냅샷의 텍스트 객체를 읽는 함수 */
    fun to(ignoreDateTime: Boolean, textLoader: () -> String): DetectionContent {
        return DetectionContent(
            textHash = selectTextHash(ignoreDateTime),
            detectionAreaSelector = detectionAreaSelector,
            imageHash = imageHash,
            textLoader = textLoader
        )
    }

    fun getAllObjectNames(): List<String> {
        return listOfNotNull(textObjectName, screenshotObjectName, highlightedScreenshotObjectName)
    }

    companion object {
        fun of(
            towerRequestHistory: TowerRequestHistory,
            textObjectName: String,
            textHash: String,
            dateTimeIgnoredTextHash: String,
            detectionAreaSelector: String?,
            imageHash: String?,
            isChangeDetected: Boolean,
            screenshotObjectName: String? = null,
            highlightedScreenshotObjectName: String? = null,
            aiDiffSummary: String? = null,
            notifiedAt: LocalDateTime? = null
        ): TowerSnapshot {
            val tower = towerRequestHistory.tower
            val snapshot = TowerSnapshot(
                tower = tower,
                towerRequestHistory = towerRequestHistory,
                textObjectName = textObjectName,
                textHash = textHash,
                dateTimeIgnoredTextHash = dateTimeIgnoredTextHash,
                detectionAreaSelector = detectionAreaSelector,
                imageHash = imageHash,
                screenshotObjectName = screenshotObjectName,
                highlightedScreenshotObjectName = highlightedScreenshotObjectName,
                isChangeDetected = isChangeDetected,
                aiDiffSummary = aiDiffSummary,
                notifiedAt = notifiedAt
            )
            tower.addSnapshot(snapshot)

            return snapshot
        }
    }
}
