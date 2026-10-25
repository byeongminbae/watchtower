package kr.byeongmin.watchtower.global.security

interface SessionMemberLoader {
    fun getMemberPrinciple(memberId: Long): MemberPrinciple
}
