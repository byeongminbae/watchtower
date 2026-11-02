package kr.byeongmin.watchtower.domain.tower.enums

/** 회원이 타워 수정 요청에서 바꿀 수 있는 타워 상태. 크롤링 대기와 일시정지만 가능함 */
enum class UpdatableTowerStatus {
    CRAWLING_READY, USER_PAUSED
}
