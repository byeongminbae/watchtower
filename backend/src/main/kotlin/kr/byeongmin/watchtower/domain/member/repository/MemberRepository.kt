package kr.byeongmin.watchtower.domain.member.repository

import jakarta.persistence.LockModeType
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.repository.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import java.time.LocalDateTime

interface MemberRepository : BaseRepository<Member, Long> {
    fun findByIdAndDeletedAtIsNull(memberId: Long): Member?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findMemberForUpdateByIdAndDeletedAtIsNull(memberId: Long): Member?

    fun findAllByEmailStartingWithOrNicknameStartingWith(
        email: String,
        nickname: String,
        pageable: Pageable
    ): Page<Member>


    fun countByDeletedAtIsNull(): Long

    fun countByBannedAtIsNullAndDeletedAtIsNull(): Long

    fun countByDeletedAtIsNotNull(): Long

    fun countByBannedAtIsNotNull(): Long

    fun countByCreatedAtAfter(createdAt: LocalDateTime): Long

    fun countByLastSignInAtAfter(lastSignInAt: LocalDateTime): Long
}
