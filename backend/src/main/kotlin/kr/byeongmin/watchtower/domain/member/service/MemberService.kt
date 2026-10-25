package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.domain.auth.accessor.NaverOAuthAccessor
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.member.dto.MemberUpdateRequestDto
import kr.byeongmin.watchtower.domain.notification.accessor.NotificationChannelAccessor
import kr.byeongmin.watchtower.domain.tower.accessor.TowerAccessor
import kr.byeongmin.watchtower.domain.tower.dto.TowerListItemResponseDto
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
    private val notificationChannelAccessor: NotificationChannelAccessor,
    private val naverOAuthAccessor: NaverOAuthAccessor,
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
    ): SuccessDataResponse<CursorPaginationResponseDto<TowerListItemResponseDto>> {
        val towers = towerAccessor.findOwnedTowers(
            memberId = memberId,
            cursorPaginationRequestDto = cursorPaginationRequestDto,
            sortField = sortField
        )

        val cursorPaginationResponseDto = cursorPaginationRequestDto.to(towers, sortField) { tower ->
            val latestAiSummary = tower.snapshots
                .filter { !it.isDeleted() && !it.isHead() }
                .maxByOrNull { it.id.ifNullThrow() }
                ?.aiDiffSummary

            TowerListItemResponseDto.from(tower, latestAiSummary)
        }

        return SuccessDataResponse(cursorPaginationResponseDto)
    }

    @Transactional
    fun updateMember(
        memberId: Long,
        memberUpdateRequestDto: MemberUpdateRequestDto
    ): SuccessDataResponse<MemberResponseDto> {
        val member = memberAccessor.getMember(memberId)

        member.updateProfile(
            nickname = memberUpdateRequestDto.nickname,
            profileImageUrl = memberUpdateRequestDto.profileImageUrl
        )

        return SuccessDataResponse(MemberResponseDto.from(member))
    }

    // TODO: 결제 넣으면 여기에 환불 로직도 넣어야됨
    @Transactional
    fun deleteMember(memberId: Long): SuccessResponse {
        val member = memberAccessor.getMember(memberId)

        naverOAuthAccessor.findByMemberId(memberId)?.delete()
        // TODO: 네이버 연동 해제(revoke) API 연동 필요 (external.naver.client.NaverAuthClient 에 revoke 메서드 추가 후 연결)
        //  조회한 NaverOAuth 의 accessToken 으로 revoke 를 호출한다.
        //  연동 해제는 네이버 쪽 정리 작업이라 실패하더라도 탈퇴(논리삭제) 자체를 막지 않는다.

        val deletedAt = timeProvider.now()
        towerAccessor.deleteAllRelatedByMemberId(memberId, deletedAt)
        notificationChannelAccessor.deleteByMemberId(memberId, deletedAt)

        member.delete()
        member.signOut()

        return SuccessResponse()
    }
}
