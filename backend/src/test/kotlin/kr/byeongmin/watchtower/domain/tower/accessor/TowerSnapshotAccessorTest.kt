package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.repository.TowerSnapshotQuerydslRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TowerSnapshotAccessorTest {
    private val towerSnapshotQuerydslRepository = mock<TowerSnapshotQuerydslRepository>()
    private val towerSnapshotAccessor = TowerSnapshotAccessor(towerSnapshotQuerydslRepository)

    @Test
    fun `소유한 타워에 스냅샷이 없으면 빈 목록을 조회할 수 있음`() {
        /** given */
        whenever(towerSnapshotQuerydslRepository.findByTowerIdAndMemberId(7L, 21L))
            .thenReturn(emptyList())

        /** when */
        val snapshots = towerSnapshotAccessor.getMemberSnapshots(towerId = 7L, memberId = 21L)

        /** then */
        assertTrue(snapshots.isEmpty())
    }

    @Test
    fun `소유한 타워를 찾을 수 없으면 스냅샷을 조회할 수 없음`() {
        /** given */
        whenever(towerSnapshotQuerydslRepository.findByTowerIdAndMemberId(7L, 21L))
            .thenReturn(null)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            towerSnapshotAccessor.getMemberSnapshots(towerId = 7L, memberId = 21L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
