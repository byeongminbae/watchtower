package kr.byeongmin.watchtower.domain.member.accessor

import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.pagination.BaseSortField
import kr.byeongmin.watchtower.global.pagination.OffsetPaginationRequestDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemberAccessorTest {
    private val memberRepository = mock<MemberRepository>()
    private val memberAccessor = MemberAccessor(memberRepository)

    @Test
    fun `회원을 찾을 수 없으면 회원 조회에 실패함`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            memberAccessor.getMember(21L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(memberRepository).findByIdAndDeletedAtIsNull(21L)
    }

    @Test
    fun `회원을 찾을 수 없으면 락 획득에 실패함`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            memberAccessor.getMemberForUpdate(21L)
        }

        /** then */
        assertEquals(CommonError.RESOURCE_NOT_FOUND, exception.error)
        verify(memberRepository).findMemberForUpdateByIdAndDeletedAtIsNull(21L)
    }

    @Test
    fun `검색어를 이메일과 닉네임 검색에 동일하게 전달함`() {
        /** given */
        val member = mock<Member>()
        val request = OffsetPaginationRequestDto(
            query = "회원",
            page = 1,
            size = 10,
            sortDirection = Sort.Direction.ASC
        )
        val pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "createdAt"))
        val page = PageImpl(listOf(member), pageable, 11L)
        whenever(
            memberRepository.findAllByEmailStartingWithOrNicknameStartingWith(
                "회원", "회원", pageable
            )
        )
            .thenReturn(page)

        /** when */
        val result = memberAccessor.findMembers(request, BaseSortField.CREATED_AT)

        /** then */
        assertEquals(page, result)
    }
}
