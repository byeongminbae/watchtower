package kr.byeongmin.watchtower.domain.admin.service

import kr.byeongmin.watchtower.domain.admin.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberRoleUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.MemberStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.admin.dto.TowerStatusUpdateRequestDto
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationResponseDto
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val towerAccessor: TowerAccessor,
    private val memberAccessor: MemberAccessor,
    private val timeProvider: TimeProvider
) {
    @Transactional
    fun updateTowerStatus(
        towerId: Long,
        towerStatusUpdateRequestDto: TowerStatusUpdateRequestDto
    ): SuccessDataResponse<TowerResponseDto> {
        val tower = towerAccessor.getTower(towerId)

        tower.changeStatusByAdmin(towerStatusUpdateRequestDto.status)

        return SuccessDataResponse(TowerResponseDto.from(tower))
    }

    @Transactional(readOnly = true)
    fun getTowers(
        offsetPaginationRequestDto: OffsetPaginationRequestDto,
        sortField: BaseSortField
    ): SuccessDataResponse<OffsetPaginationResponseDto<TowerResponseDto>> {
        val towerPages = towerAccessor.findTowers(
            offsetPaginationRequestDto = offsetPaginationRequestDto,
            sortField = sortField
        )

        return SuccessDataResponse(
            OffsetPaginationResponseDto.from(towerPages) {
                TowerResponseDto.from(it)
            }
        )
    }

    /** 탈퇴한 회원도 조회됨 */
    @Transactional(readOnly = true)
    fun getMembers(
        offsetPaginationRequestDto: OffsetPaginationRequestDto,
        sortField: BaseSortField
    ): SuccessDataResponse<OffsetPaginationResponseDto<MemberResponseDto>> {
        val members = memberAccessor.findMembers(
            offsetPaginationRequestDto = offsetPaginationRequestDto,
            sortField = sortField
        )

        return SuccessDataResponse(
            OffsetPaginationResponseDto.from(members) {
                MemberResponseDto.from(it)
            }
        )
    }

    @Transactional
    fun updateMemberRole(
        memberId: Long,
        memberRoleUpdateRequestDto: MemberRoleUpdateRequestDto
    ): SuccessDataResponse<MemberResponseDto> {
        val member = memberAccessor.getMember(memberId)

        member.changeRole(memberRoleUpdateRequestDto.role)

        return SuccessDataResponse(MemberResponseDto.from(member))
    }

    @Transactional
    fun updateMemberStatus(
        memberId: Long,
        memberStatusUpdateRequestDto: MemberStatusUpdateRequestDto
    ): SuccessDataResponse<MemberResponseDto> {
        val member = memberAccessor.getMember(memberId)

        if (memberStatusUpdateRequestDto.banned) {
            member.ban(timeProvider.now(), memberStatusUpdateRequestDto.banReason)
        } else {
            member.unban()
        }

        return SuccessDataResponse(MemberResponseDto.from(member))
    }

}
