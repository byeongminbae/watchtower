package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.repository.TowerConditionQuerydslRepository
import kr.byeongmin.watchtower.domain.tower.repository.TowerConditionRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TowerConditionAccessorTest {
    private val repository = mock<TowerConditionRepository>()
    private val querydslRepository = mock<TowerConditionQuerydslRepository>()
    private val accessor = TowerConditionAccessor(repository, querydslRepository)

    @Test
    fun `소유한 타워에서 감지 조건을 찾지 못하면 조회 실패로 처리함`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            accessor.getOwnedCondition(conditionId = 3L, towerId = 7L, memberId = 21L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(repository).findByIdAndTower_IdAndTower_CreatedBy_IdAndDeletedAtIsNullAndTower_DeletedAtIsNull(
            3L, 7L, 21L
        )
    }

    @Test
    fun `소유한 타워가 있으면 감지 조건이 없어도 빈 목록을 조회할 수 있음`() {
        /** given */
        whenever(querydslRepository.findByTowerIdAndMemberId(7L, 21L)).thenReturn(emptyList())

        /** when */
        val conditions = accessor.getOwnedTowerConditions(towerId = 7L, memberId = 21L)

        /** then */
        assertTrue(conditions.isEmpty())
    }

    @Test
    fun `소유한 타워가 없으면 감지 조건 목록을 조회할 수 없음`() {
        /** given */
        whenever(querydslRepository.findByTowerIdAndMemberId(7L, 21L)).thenReturn(null)

        /** when */
        val exception = assertFailsWith<BusinessException> {
            accessor.getOwnedTowerConditions(towerId = 7L, memberId = 21L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
