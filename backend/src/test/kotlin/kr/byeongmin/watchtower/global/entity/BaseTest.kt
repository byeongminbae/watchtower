package kr.byeongmin.watchtower.global.entity

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test

class BaseTest {
    private val entity = object : Base() {
        override val id: Long? = null
    }

    @Test
    fun `삭제한 데이터는 삭제 시각으로 구분할 수 있음`() {
        /** when */
        entity.delete()

        /** then */
        assertNotNull(entity.deletedAt)
    }

    @Test
    fun `이미 삭제한 데이터를 다시 삭제할 수 없고 최초 삭제 시각은 유지됨`() {
        /** given */
        entity.delete()
        val originalDeletedAt = entity.deletedAt

        /** when */
        val exception = assertFailsWith<BusinessException> {
            entity.delete()
        }

        /** then */
        assertEquals(CommonError.RESOURCE_ALREADY_DELETED, exception.error)
        assertEquals(originalDeletedAt, entity.deletedAt)
    }
}
