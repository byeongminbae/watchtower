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
    fun findMember(memberId: Long): Member? {
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
    }

    fun getMember(memberId: Long): Member {
        return findMember(memberId)
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

    fun countMembers(): Long {
        return memberRepository.countByDeletedAtIsNull()
    }

    fun countActiveMembers(): Long {
        return memberRepository.countByBannedAtIsNullAndDeletedAtIsNull()
    }

    fun countDeletedMembers(): Long {
        return memberRepository.countByDeletedAtIsNotNull()
    }

    fun countBannedMembers(): Long {
        return memberRepository.countByBannedAtIsNotNull()
    }

    fun countSignedUpMembers(signedUpAfter: LocalDateTime): Long {
        return memberRepository.countByCreatedAtAfter(signedUpAfter)
    }

    fun countSignedInMembers(signedInAfter: LocalDateTime): Long {
        return memberRepository.countByLastSignInAtAfter(signedInAfter)
    }
}
