package kr.byeongmin.watchtower.domain.tower.enums

/**
 * @property [READY] 이전 크롤링에 성공해 다음 크롤링을 기다리는 상태
 * @property [ERROR] 이전 크롤링에 실패한 상태. 회원이 재시도해야 다시 크롤링함
 * @property [RUNNING] 크롤링 요청을 큐에 넣고 결과를 기다리는 상태. 새로 등록한 타워도 이 상태로 시작함
 * @property [PAUSED] 회원이 일시정지한 상태. 장기 미접속 회원의 타워도 이 상태가 되며 회원이 크롤링 대기로 되돌릴 수 있음
 * @property [SUSPENDED] 관리자가 정지한 상태(주로 불법적인 내용). 논리 삭제는 가능하지만 관리자만 풀 수 있음
 */
enum class TowerStatus {
    READY, ERROR, RUNNING, PAUSED, SUSPENDED;

    fun isMemberChangeable(): Boolean {
        return this != SUSPENDED
    }

    fun isAdminChangeable(): Boolean {
        return this != RUNNING
    }

    fun isPausable(): Boolean {
        return this == READY || this == ERROR || this == RUNNING
    }

    fun isResumable(): Boolean {
        return this == PAUSED
    }
}
