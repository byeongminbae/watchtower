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
    fun `엔티티를 찾으면 기본 오류 처리 없이 반환함`() {
        /** given */
        val entity = TestEntity()
        whenever(repository.find(1L)).thenReturn(entity)

        /** when */
        val found = repository.get(1L)

        /** then */
        assertSame(entity, found)
    }

    @Test
    fun `데이터를 찾을 수 없으면 기본 오류나 지정한 오류로 처리할 수 있음`() {
        /** given */
        whenever(repository.find(1L)).thenReturn(null)
        val expected = IllegalStateException("조회 실패")

        /** when */
        val found = repository.find(1L)
        val exception = assertFailsWith<BusinessException> {
            repository.get(1L)
        }
        val customException = assertFailsWith<IllegalStateException> {
            repository.get(1L) { throw expected }
        }

        /** then */
        assertNull(found)
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        assertSame(expected, customException)
    }

    @Test
    fun `엔티티를 찾으면 지정한 조회 실패 처리를 실행하지 않음`() {
        /** given */
        val entity = TestEntity()
        whenever(repository.find(1L)).thenReturn(entity)

        /** when */
        val found = repository.get(1L) { error("호출되면 안 됨") }

        /** then */
        assertSame(entity, found)
    }

    @Test
    fun `삭제 포함 조회에서는 논리 삭제한 엔티티도 찾을 수 있음`() {
        /** given */
        val entity = TestEntity().apply { delete() }
        whenever(repository.findById(1L)).thenReturn(Optional.of(entity))

        /** when, then */
        assertSame(entity, repository.findAny(1L))
        assertSame(entity, repository.getAny(1L))
        assertSame(entity, repository.getAny(1L) { error("호출되면 안 됨") })
    }

    @Test
    fun `삭제 포함 조회에서도 엔티티가 없으면 요청한 방식으로 조회 실패를 처리함`() {
        /** given */
        whenever(repository.findById(1L)).thenReturn(Optional.empty())
        val expected = IllegalStateException("조회 실패")

        /** when, then */
        assertNull(repository.findAny(1L))
        val exception = assertFailsWith<BusinessException> {
            repository.getAny(1L)
        }
        val customException = assertFailsWith<IllegalStateException> {
            repository.getAny(1L) { throw expected }
        }

        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        assertSame(expected, customException)
    }
}
