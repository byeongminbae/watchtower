package kr.byeongmin.watchtower.global.repository

import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.Answers.CALLS_REAL_METHODS
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class BaseRepositoryTest {
    private class TestEntity(override val id: Long = 1L) : Base()

    private val repository = mock<BaseRepository<TestEntity, Long>>(defaultAnswer = CALLS_REAL_METHODS)

    @Test
    fun `엔티티를 찾으면 반환함`() {
        /** given */
        val entity = TestEntity()
        whenever(repository.find(1L)).thenReturn(entity)

        /** when */
        val found = repository.get(1L)

        /** then */
        assertSame(entity, found)
    }

    @Test
    fun `엔티티를 찾을 수 없으면 find는 null을 반환하고 get은 조회 실패로 처리함`() {
        /** given */
        whenever(repository.find(1L)).thenReturn(null)

        /** when */
        val found = repository.find(1L)
        val exception = assertFailsWith<BusinessException> {
            repository.get(1L)
        }

        /** then */
        assertNull(found)
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }

    @Test
    fun `삭제 포함 조회에서는 논리 삭제한 엔티티도 찾을 수 있음`() {
        /** given */
        val entity = TestEntity().apply { delete() }
        whenever(repository.findById(1L)).thenReturn(Optional.of(entity))

        /** when */
        val found = repository.findAny(1L)
        val required = repository.getAny(1L)

        /** then */
        assertSame(entity, found)
        assertSame(entity, required)
    }

    @Test
    fun `삭제 포함 조회에서도 엔티티가 없으면 findAny는 null을 반환하고 getAny는 조회 실패로 처리함`() {
        /** given */
        whenever(repository.findById(1L)).thenReturn(Optional.empty())

        /** when */
        val found = repository.findAny(1L)
        val exception = assertFailsWith<BusinessException> {
            repository.getAny(1L)
        }

        /** then */
        assertNull(found)
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
    }
}
