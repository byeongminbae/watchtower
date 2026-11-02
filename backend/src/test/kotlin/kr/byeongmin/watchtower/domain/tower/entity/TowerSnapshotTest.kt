package kr.byeongmin.watchtower.domain.tower.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertSame

class TowerSnapshotTest {
    private val tower = mock<Tower>()
    private val url = "https://example.com/snapshot"

    @Test
    fun `새 스냅샷을 만들면 해당 타워에도 스냅샷을 연결함`() {
        /** when */
        val snapshot = TowerSnapshot.from(tower, url)

        /** then */
        assertSame(tower, snapshot.tower)
        verify(tower).addSnapshot(snapshot)
    }
}
