package kr.byeongmin.watchtower.domain.tower.enums

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class TowerStatusTest {
    @Test
    fun `회원은 실행 중인 타워의 상태를 변경할 수 있음`() {
        /** when */
        val result = TowerStatus.RUNNING.isMemberChangeable()

        /** then */
        assertTrue(result)
    }

    @Test
    fun `회원은 일시정지한 타워의 상태를 변경할 수 있음`() {
        /** when */
        val result = TowerStatus.PAUSED.isMemberChangeable()

        /** then */
        assertTrue(result)
    }

    @Test
    fun `정책 위반으로 정지된 타워는 회원이 상태 변경 불가`() {
        /** when */
        val result = TowerStatus.ILLEGAL_SUSPENDED.isMemberChangeable()

        /** then */
        assertFalse(result)
    }
}
