package kr.byeongmin.watchtower.domain.tower.vo

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DetectionContentTest {
    private fun content(
        textHash: String = "a".repeat(64),
        detectionAreaSelector: String? = null,
        imageHash: String? = null,
        textLoader: () -> String = { "재고 있음" }
    ): DetectionContent {
        return DetectionContent(
            textHash = textHash,
            detectionAreaSelector = detectionAreaSelector,
            imageHash = imageHash,
            textLoader = textLoader
        )
    }

    @Test
    fun `텍스트 해시와 감시 영역과 이미지 해시가 모두 같아야 같은 내용으로 봄`() {
        /** given */
        val previous = content(detectionAreaSelector = ".stock", imageHash = "b".repeat(64))

        /** when */
        val results = listOf(
            previous.hasSameContent(content(detectionAreaSelector = ".stock", imageHash = "b".repeat(64))),
            previous.hasSameContent(content(textHash = "c".repeat(64), detectionAreaSelector = ".stock", imageHash = "b".repeat(64))),
            previous.hasSameContent(content(detectionAreaSelector = ".price", imageHash = "b".repeat(64))),
            previous.hasSameContent(content(detectionAreaSelector = ".stock", imageHash = null))
        )

        /** then */
        assertEquals(listOf(true, false, false, false), results)
    }

    @Test
    fun `감시 영역이 같아야 같은 범위에서 뽑은 내용으로 봄`() {
        /** when, then */
        assertTrue(content(detectionAreaSelector = ".stock").hasSameDetectionArea(content(detectionAreaSelector = ".stock")))
        assertFalse(content(detectionAreaSelector = ".stock").hasSameDetectionArea(content(detectionAreaSelector = null)))
    }

    @Test
    fun `텍스트 해시가 다르면 텍스트가 달라졌다고 봄`() {
        /** given */
        val previous = content(textHash = "a".repeat(64))

        /** when */
        val changed = previous.hasDifferentText(content(textHash = "b".repeat(64)))
        val unchanged = previous.hasDifferentText(content(textHash = "a".repeat(64)))

        /** then */
        assertTrue(changed)
        assertFalse(unchanged)
    }

    @Test
    fun `이미지를 수집하지 않은 버전과는 이미지가 달라졌다고 보지 않음`() {
        /** when */
        val changed = content(imageHash = "a".repeat(64)).hasDifferentImage(content(imageHash = "b".repeat(64)))
        val notCollectedBefore = content(imageHash = null).hasDifferentImage(content(imageHash = "b".repeat(64)))

        /** then */
        assertTrue(changed)
        assertFalse(notCollectedBefore)
    }

    @Test
    fun `텍스트는 처음 읽을 때 한 번만 불러옴`() {
        /** given */
        var loadCount = 0
        val content = content(textLoader = {
            loadCount++
            "재고 있음"
        })

        /** when */
        val beforeRead = loadCount
        val texts = listOf(content.text, content.text)

        /** then */
        assertEquals(0, beforeRead)
        assertEquals(listOf("재고 있음", "재고 있음"), texts)
        assertEquals(1, loadCount)
    }
}
