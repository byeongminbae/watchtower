package kr.byeongmin.watchtower.domain.tower.enums

enum class TowerStatus {
    RUNNING, PAUSED, ILLEGAL_SUSPENDED;

    /** 어드민이 아닌 회원이 직접 전환할 수 있는 상태인지 확인 */
    fun isMemberChangeable(): Boolean {
        return this == RUNNING || this == PAUSED
    }
}
