package kr.byeongmin.watchtower.domain.tower.error

import kr.byeongmin.watchtower.global.error.Error

/** 타워 생성·수정 및 조건 등록에서 발생하는 도메인 오류. */
enum class TowerError(
    override val statusCode: String,
    override val message: String
) : Error {
    INTERVAL_INVALID("TOW_001", "확인 주기는 5분 단위로 5분 이상 24시간 이하여야 합니다."),
    CONDITION_QUOTA_EXCEEDED("TOW_002", "타워 하나에 등록할 수 있는 조건은 최대 3개입니다."),
    URL_NOT_ALLOWED("TOW_003", "내부망 등 허용되지 않는 주소는 등록할 수 없습니다."),
    UNSUPPORTED_CONDITION("TOW_004", "지원하지 않는 타워 조건입니다.");

    override fun toString(): String {
        return "TowerError(statusCode='$statusCode', message='$message')"
    }
}
