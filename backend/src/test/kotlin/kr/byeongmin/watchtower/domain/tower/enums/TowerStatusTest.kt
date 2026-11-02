package kr.byeongmin.watchtower.domain.tower.enums

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class TowerStatusTest {
    @ParameterizedTest
    @EnumSource(TowerStatus::class, names = ["ADMIN_PAUSED"], mode = EnumSource.Mode.EXCLUDE)
    fun `회원은 관리자가 정지한 타워가 아니면 상태를 변경할 수 있음`(status: TowerStatus) {
        /** when */
        val result = status.isMemberChangeable()

        /** then */
        assertTrue(result)
    }

    @Test
    fun `관리자가 정지한 타워는 회원이 상태 변경 불가`() {
        /** when */
        val result = TowerStatus.ADMIN_PAUSED.isMemberChangeable()

        /** then */
        assertFalse(result)
    }
}
