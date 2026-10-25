package kr.byeongmin.watchtower.domain.tower.entity

import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertSame

class TowerSnapshotTest {
    private val tower = mock<Tower>()
    private val url = "https://example.com/snapshot"

    @Test
    fun `본문만 저장한 최초 수집본은 변경 기록으로 보지 않음`() {
        /** given */
        val snapshot = TowerSnapshot.from(tower, url)

        /** when */
        val isHead = snapshot.isHead()

        /** then */
        assertTrue(isHead)
    }

    @Test
    fun `변경 결과가 있는 스냅샷은 최초 수집본과 구분함`() {
        /** given */
        val snapshots = listOf(
            TowerSnapshot.from(tower, url, htmlContentDiffUrl = url),
            TowerSnapshot.from(tower, url, screenshotUrl = url),
            TowerSnapshot.from(tower, url, screenshotDiffUrl = url),
            TowerSnapshot.from(tower, url, aiDiffSummary = "변경 요약"),
            TowerSnapshot.from(tower, url, notifiedAt = LocalDateTime.of(2026, 10, 20, 12, 0))
        )

        snapshots.forEach { snapshot ->
            /** when */
            val isHead = snapshot.isHead()

            /** then */
            assertFalse(isHead)
        }
    }

    @Test
    fun `새 스냅샷을 만들면 해당 타워에도 스냅샷을 연결함`() {
        /** when */
        val snapshot = TowerSnapshot.from(tower, url)

        /** then */
        assertSame(tower, snapshot.tower)
        verify(tower).addSnapshot(snapshot)
    }
}
