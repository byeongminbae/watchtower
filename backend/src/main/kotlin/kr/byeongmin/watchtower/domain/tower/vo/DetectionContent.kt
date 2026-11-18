package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.global.utils.isNotNull

/**
 * 감지 판정에 쓰이는 값 모음
 *
 * @property [text] 키워드 조건처럼 텍스트가 필요한 조건이 처음 읽을 때 [textLoader] 로 버킷에서 읽음
 */
class DetectionContent(
    val textHash: String,
    val detectionAreaSelector: String?,
    val imageHash: String?,
    textLoader: () -> String
) {
    val text: String by lazy(textLoader)

    fun hasSameContent(current: DetectionContent): Boolean {
        return textHash == current.textHash &&
                detectionAreaSelector == current.detectionAreaSelector &&
                imageHash == current.imageHash
    }

    fun hasSameDetectionArea(current: DetectionContent): Boolean {
        return detectionAreaSelector == current.detectionAreaSelector
    }

    fun hasDifferentText(current: DetectionContent): Boolean {
        return textHash != current.textHash
    }

    /** 이미지 감지를 켜기 전 버전은 이미지를 수집하지 않아서 비교할 수 없음 */
    fun hasDifferentImage(current: DetectionContent): Boolean {
        return imageHash.isNotNull() && current.imageHash.isNotNull() && imageHash != current.imageHash
    }
}
