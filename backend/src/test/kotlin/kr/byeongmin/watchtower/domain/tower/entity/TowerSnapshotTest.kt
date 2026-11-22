package kr.byeongmin.watchtower.domain.tower.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import kotlin.test.*

class TowerSnapshotTest {
    private val tower = mock<Tower>()
    private val towerRequestHistory = mock<TowerRequestHistory> {
        on { tower } doReturn tower
    }
    private val textObjectName = "towers/7/2026-11-04T01:00:00.000Z.txt"

    private fun snapshot(screenshotObjectName: String? = null): TowerSnapshot {
        return TowerSnapshot.of(
            towerRequestHistory = towerRequestHistory,
            textObjectName = textObjectName,
            textHash = "a".repeat(64),
            dateTimeIgnoredTextHash = "d".repeat(64),
            detectionAreaSelector = ".stock",
            imageHash = "b".repeat(64),
            isChangeDetected = false,
            screenshotObjectName = screenshotObjectName
        )
    }

    @Test
    fun `새 스냅샷을 만들면 요청 이력의 타워에 스냅샷을 연결하고 받아온 해시와 감시 영역을 담음`() {
        /** when */
        val snapshot = snapshot()

        /** then */
        assertSame(tower, snapshot.tower)
        assertSame(towerRequestHistory, snapshot.towerRequestHistory)
        assertEquals(".stock", snapshot.detectionAreaSelector)
        verify(tower).addSnapshot(snapshot)
    }

    @Test
    fun `감시 영역이 같아야 같은 영역에서 뽑은 버전으로 봄`() {
        /** given */
        val snapshot = snapshot()

        /** when, then */
        assertTrue(snapshot.hasSameDetectionArea(".stock"))
        assertFalse(snapshot.hasSameDetectionArea(".price"))
        assertFalse(snapshot.hasSameDetectionArea(null))
    }

    @Test
    fun `날짜를 무시하면 날짜와 시간을 뺀 해시로 비교 내용을 만들고 아니면 원문 해시로 만듦`() {
        /** given */
        val snapshot = snapshot()

        /** when */
        val ignoringDateTime = snapshot.to(ignoreDateTime = true) { "재고 있음" }
        val notIgnoringDateTime = snapshot.to(ignoreDateTime = false) { "재고 있음" }

        /** then */
        assertEquals("d".repeat(64), ignoringDateTime.textHash)
        assertEquals(".stock", ignoringDateTime.detectionAreaSelector)
        assertEquals("a".repeat(64), notIgnoringDateTime.textHash)
    }

    @Test
    fun `버킷 객체 이름에는 텍스트와 만들어진 스크린샷만 담김`() {
        /** given */
        val snapshot = snapshot(screenshotObjectName = "towers/7/screenshot.png")

        /** when */
        val objectNames = snapshot.getAllObjectNames()

        /** then */
        assertEquals(listOf(textObjectName, "towers/7/screenshot.png"), objectNames)
    }

    @Test
    fun `변경 알림을 보낸 시각을 스냅샷과 타워에 함께 남김`() {
        /** given */
        val snapshot = snapshot()
        val notifiedAt = LocalDateTime.of(2026, 11, 21, 3, 0)

        /** when */
        snapshot.markNotified(notifiedAt)

        /** then */
        assertEquals(notifiedAt, snapshot.notifiedAt)
        verify(tower).lastNotifiedAt = notifiedAt
    }
}
