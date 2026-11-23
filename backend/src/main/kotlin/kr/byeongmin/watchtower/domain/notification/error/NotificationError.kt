package kr.byeongmin.watchtower.domain.notification.error

import kr.byeongmin.watchtower.global.error.Error

enum class NotificationError(
    override val statusCode: String,
    override val message: String
) : Error {
    RECIPIENT_UNREACHABLE("NTF_001", "알림을 받을 수 없는 회원입니다.");

    override fun toString(): String {
        return "NotificationError(statusCode='$statusCode', message='$message')"
    }
}
