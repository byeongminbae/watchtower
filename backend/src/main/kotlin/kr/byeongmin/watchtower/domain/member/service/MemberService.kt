package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.member.dto.MemberUpdateRequestDto
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerSnapshotAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.CursorPaginationRequestDto
import kr.byeongmin.watchtower.global.pagination.CursorPaginationResponseDto
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberAccessor: MemberAccessor,
    private val towerAccessor: TowerAccessor,
    private val towerSnapshotAccessor: TowerSnapshotAccessor,
    private val timeProvider: TimeProvider
) {
    fun getMember(memberId: Long): SuccessDataResponse<MemberResponseDto> {
        val member = memberAccessor.getMember(memberId)
        val memberResponseDto = MemberResponseDto.from(member)
        return SuccessDataResponse(memberResponseDto)
    }

    @Transactional(readOnly = true)
    fun getMemberTowers(
        memberId: Long,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        sortField: BaseSortField
    ): SuccessDataResponse<CursorPaginationResponseDto<TowerResponseDto>> {
        val towers = towerAccessor.findOwnedTowers(
            memberId = memberId,
            cursorPaginationRequestDto = cursorPaginationRequestDto,
            sortField = sortField
        )

        val latestChangeAiSummaryByTowerId = towerSnapshotAccessor.findLatestChangeAiSummaries(
            towers.map { it.id.ifNullThrow() }
        ).associateBy { it.towerId }

        val cursorPaginationResponseDto = cursorPaginationRequestDto.to(towers, sortField) { tower ->
            TowerResponseDto.from(tower, latestChangeAiSummaryByTowerId[tower.id]?.aiSummary)
        }

        return SuccessDataResponse(cursorPaginationResponseDto)
    }

    @Transactional
    fun updateMember(
        memberId: Long,
        memberUpdateRequestDto: MemberUpdateRequestDto
    ): SuccessDataResponse<MemberResponseDto> {
        val member = memberAccessor.getMember(memberId)

        member.nickname = memberUpdateRequestDto.nickname
        member.profileImageUrl = memberUpdateRequestDto.profileImageUrl

        return SuccessDataResponse(MemberResponseDto.from(member))
    }

    // TODO: 결제 넣으면 여기에 환불 로직도 넣어야됨
    @Transactional
    fun deleteMember(memberId: Long): SuccessResponse {
        val member = memberAccessor.getMember(memberId)

        val deletedAt = timeProvider.now()
        towerAccessor.deleteAllOwnedTowers(memberId, deletedAt)

        member.delete()

        return SuccessResponse()
    }
}
