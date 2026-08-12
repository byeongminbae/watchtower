package kr.byeongmin.watchtower.domain.member.repository

import kr.byeongmin.watchtower.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<Member, Long> {
    
}