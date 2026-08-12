package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.domain.member.dto.MemberResponseDto
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.response.SuccessDataResponse
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository
) {
    fun getMember(memberId: Long): SuccessDataResponse<MemberResponseDto> {
        if (!memberRepository.existsById(memberId)) {
            throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
        }
        val dto = MemberResponseDto.from(memberRepository.findById(memberId).get())
        return SuccessDataResponse(dto)
    }
}