package kr.byeongmin.watchtower.global.pagination

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

data class OffsetPaginationRequestDto(
    @field:Schema(description = "검색어(빈 문자열이면 전체 요청)", example = "watchtower")
    val query: String = "",
    @field:Schema(description = "0부터 시작하는 페이지 번호", example = "0", minimum = "0", defaultValue = "0")
    @field:Min(0)
    val page: Int = 0,
    @field:Schema(description = "한 페이지당 최대 아이템 갯수", example = "20", minimum = "1", maximum = "50")
    @field:Min(1)
    @field:Max(50)
    val size: Int = 20,
    @field:Schema(
        description = "정렬 방향"
    )
    val sortDirection: Sort.Direction = Sort.Direction.DESC
) {
    fun to(sortField: SortField): Pageable {
        return PageRequest.of(page, size, sortField.to(sortDirection))
    }
}
