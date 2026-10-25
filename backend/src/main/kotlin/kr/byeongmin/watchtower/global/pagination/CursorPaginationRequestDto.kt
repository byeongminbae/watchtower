package kr.byeongmin.watchtower.global.pagination

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import kr.byeongmin.watchtower.global.utils.isNull
import org.springframework.data.domain.Sort

data class CursorPaginationRequestDto(
    @field:Schema(description = "검색어. 빈 문자열이면 검색어로 필터링하지 않음")
    val query: String = "",

    @field:Schema(
        description = "직전 응답의 nextCursorId. 첫 페이지는 생략",
        example = "120",
        nullable = true
    )
    val cursorId: Long? = null,

    @field:Schema(description = "직전 응답의 nextCursorValue. ID 정렬이면 null이고 첫 페이지는 생략")
    val cursorValue: String? = null,

    @field:Schema(description = "한 페이지에 반환할 최대 항목 수", example = "20", minimum = "1", maximum = "50")
    @field:Min(1)
    @field:Max(50)
    val size: Int = 20,

    @field:Schema(
        description = "선택한 정렬 기준의 방향. 값이 같은 항목은 ID를 같은 방향으로 정렬함",
        example = "DESC",
        defaultValue = "DESC",
        allowableValues = ["ASC", "DESC"]
    )
    val sortDirection: Sort.Direction = Sort.Direction.DESC
) {
    /** 다음 페이지가 존재하는지 체크하기 위해 +1 조회 */
    fun queryLimit(): Long {
        return size.toLong() + 1
    }

    fun isSortDirectionAsc(): Boolean {
        return sortDirection == Sort.Direction.ASC
    }

    /** [cursorValue] 는 ID 정렬이면 null 이기 때문에 조건에 넣지 않음 */
    fun isFirstPage(): Boolean {
        return cursorId.isNull()
    }

    /** 조회 결과를 요청한 크기로 자르고 다음 커서와 다음 페이지 존재 여부를 계산 */
    fun <T : Base, R> to(
        queriedData: List<T>,
        sortField: SortField,
        transform: (T) -> R
    ): CursorPaginationResponseDto<R> {
        val hasNext = queriedData.size > size
        val data = queriedData.take(size)
        val last = if (hasNext) data.last() else null

        return CursorPaginationResponseDto(
            data = data.map(transform),
            size = size,
            nextCursorId = last?.let { it.id.ifNullThrow() },
            nextCursorValue = last?.let { sortField.getCursorValue(it) },
            hasNext = hasNext
        )
    }
}
