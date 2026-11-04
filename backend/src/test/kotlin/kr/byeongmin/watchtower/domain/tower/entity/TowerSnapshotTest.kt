package kr.byeongmin.watchtower.domain.tower.entity

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertSame

class TowerSnapshotTest {
    private val tower = mock<Tower>()
    private val towerRequestHistory = mock<TowerRequestHistory> {
        on { tower } doReturn tower
    }
    private val htmlObjectName = "towers/7/2026-11-04T01:00:00.000Z.html"

    @Test
    fun `새 스냅샷을 만들면 요청 이력의 타워에 스냅샷을 연결함`() {
        /** when */
        val snapshot = TowerSnapshot.from(
            towerRequestHistory = towerRequestHistory,
            htmlObjectName = htmlObjectName,
            htmlSha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            isConditionSatisfied = false
        )

        /** then */
        assertSame(tower, snapshot.tower)
        assertSame(towerRequestHistory, snapshot.towerRequestHistory)
        verify(tower).addSnapshot(snapshot)
    }
}
