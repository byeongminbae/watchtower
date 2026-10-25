package kr.byeongmin.watchtower.global.pagination

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

data class OffsetPaginationResponseDto<T>(
    @field:Schema(description = "현재 페이지의 항목 목록")
    val data: List<T>,
    @field:Schema(description = "현재 페이지 번호", minimum = "0")
    val page: Int,
    @field:Schema(description = "요청한 페이지 크기", minimum = "1", example = "20")
    val size: Int,
    @field:Schema(description = "검색 조건에 일치하는 전체 항목 수", example = "135")
    val totalElements: Long,
    @field:Schema(description = "검색 조건에 일치하는 전체 페이지 수", example = "7")
    val totalPages: Int
) {
    companion object {
        fun <T : Any, R> from(page: Page<T>, transform: (T) -> R): OffsetPaginationResponseDto<R> {
            return OffsetPaginationResponseDto(
                data = page.content.map(transform),
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
        }
    }
}
