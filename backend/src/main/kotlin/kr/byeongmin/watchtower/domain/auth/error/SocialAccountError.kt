package kr.byeongmin.watchtower.domain.auth.error

import kr.byeongmin.watchtower.global.error.Error

enum class SocialAccountError(
    override val statusCode: String,
    override val message: String
) : Error {
    SOCIAL_ACCOUNT_NOT_LINKED("IDP_001", "알림을 받을 계정이 연결되어 있지 않습니다."),
    LINKED_TO_OTHER_MEMBER("IDP_002", "다른 회원에 연결된 계정입니다."),
    PROVIDER_ALREADY_LINKED("IDP_003", "같은 서비스의 계정이 이미 연결되어 있습니다."),
    DISCORD_DIRECT_MESSAGE_BLOCKED(
        "IDP_004",
        "Discord 개인정보 설정에서 서버 멤버의 다이렉트 메시지를 허용해야 알림을 받을 수 있습니다."
    );

    override fun toString(): String {
        return "SocialAccountError(statusCode='$statusCode', message='$message')"
    }
}
