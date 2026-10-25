package kr.byeongmin.watchtower.global.security

/** JWT에서 추출해 인증 컨텍스트에 저장하는 객체 */
class MemberPrinciple(
    val memberId: Long,
    val role: String
)