package kr.byeongmin.watchtower.domain.tower.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertSame

class TowerSnapshotTest {
    private val tower = mock<Tower>()
    private val htmlObjectName = "towers/7/2026-11-04T01:00:00.000Z.html"

    @Test
    fun `새 스냅샷을 만들면 해당 타워에도 스냅샷을 연결함`() {
        /** when */
        val snapshot = TowerSnapshot.from(tower = tower, htmlObjectName = htmlObjectName, htmlSha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", changed = false)

        /** then */
        assertSame(tower, snapshot.tower)
        verify(tower).addSnapshot(snapshot)
    }
}
