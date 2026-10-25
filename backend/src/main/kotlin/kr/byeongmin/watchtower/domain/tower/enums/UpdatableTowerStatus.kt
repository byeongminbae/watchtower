package kr.byeongmin.watchtower.domain.tower.enums

/** 회원이 수정 요청에서 선택할 수 있는 타워 상태 */
enum class UpdatableTowerStatus(
    val towerStatus: TowerStatus
) {
    RUNNING(TowerStatus.RUNNING),
    PAUSED(TowerStatus.PAUSED)
}
