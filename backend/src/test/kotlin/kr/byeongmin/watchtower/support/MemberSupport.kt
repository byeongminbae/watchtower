package kr.byeongmin.watchtower.support

import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.domain.member.entity.Member

class MemberSupport {
    fun createMember(profile: NaverProfileResponseDto, id: Long = 1L): Member {
        val member = Member.from(profile)
        Member::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(member, id)
        }
        return member
    }
}
