package kr.byeongmin.watchtower.global.pagination

import io.swagger.v3.oas.annotations.media.Schema

data class CursorPaginationResponseDto<T>(
    @field:Schema(description = "현재 페이지의 항목 목록")
    val data: List<T>,
    @field:Schema(description = "요청한 페이지 크기", example = "20")
    val size: Int,
    @field:Schema(
        description = "다음 요청의 cursorId. 다음 페이지가 없으면 null",
        example = "101",
        nullable = true
    )
    val nextCursorId: Long?,
    @field:Schema(description = "다음 요청의 cursorValue. ID 정렬이거나 정렬값이 null이면 null")
    val nextCursorValue: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean
)
