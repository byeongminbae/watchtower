package kr.byeongmin.watchtower.domain.member.accessor

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MemberAccessor(
    private val memberRepository: MemberRepository
) {
    fun getMember(memberId: Long): Member {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun getMemberForUpdate(memberId: Long): Member {
        return memberRepository.findMemberForUpdateByIdAndDeletedAtIsNull(memberId)
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }

    fun save(member: Member): Member {
        return memberRepository.save(member)
    }

    fun findMembers(offsetPaginationRequestDto: OffsetPaginationRequestDto, sortField: BaseSortField): Page<Member> {
        return memberRepository.findAllByEmailStartingWithOrNicknameStartingWith(
            offsetPaginationRequestDto.query,
            offsetPaginationRequestDto.query,
            offsetPaginationRequestDto.to(sortField)
        )
    }

    fun count(): Long {
        return memberRepository.countByDeletedAtIsNull()
    }

    fun countActive(): Long {
        return memberRepository.countByBannedAtIsNullAndDeletedAtIsNull()
    }

    fun countDeleted(): Long {
        return memberRepository.countByDeletedAtIsNotNull()
    }

    fun countBanned(): Long {
        return memberRepository.countByBannedAtIsNotNull()
    }

    fun countByCreatedAtAfter(createdAt: LocalDateTime): Long {
        return memberRepository.countByCreatedAtAfter(createdAt)
    }

    fun countByLastSignInAtAfter(lastSignInAt: LocalDateTime): Long {
        return memberRepository.countByLastSignInAtAfter(lastSignInAt)
    }
}
