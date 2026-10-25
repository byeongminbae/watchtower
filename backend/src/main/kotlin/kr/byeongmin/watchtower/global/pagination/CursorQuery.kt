package kr.byeongmin.watchtower.global.pagination

import com.querydsl.core.types.Ops
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.ComparableExpressionBase
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.NumberPath
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.utils.ifNullThrow

/**
 * 커서 페이지네이션 조회 조건과 정렬 순서를 만드는 객체
 *
 * 어떤 필드를 기준으로 정렬할지와 cursorValue를 어떤 타입으로 바꿀지는 정렬 enum 에 정의하고
 * [CursorQuery] 는 전달받은 필드와 값으로 커서 페이지네이션에서 사용될 비교 조건을 만듦
 *
 * createdAt deletedAt 같은 날짜 외에 문자열이나 숫자로 정렬하는 경우에도 DTO 나 정렬 enum 에 비교 로직을 반복해서 넣지 않도록 분리함
 */
object CursorQuery {
    /**
     * 정렬 기준이 ID 일 때 WHERE 절의 커서 검색 조건을 만드는 메서드
     *
     * ASC 면 id > cursorId, DESC 면 id < cursorId 로 조회함
     */
    fun createIdCursorCondition(
        idPath: NumberPath<Long>,
        cursorPaginationRequestDto: CursorPaginationRequestDto
    ): BooleanExpression? {
        if (cursorPaginationRequestDto.isFirstPage()) {
            return null
        }

        return createComparisonBySortDirection(
            idPath,
            cursorPaginationRequestDto.cursorId.ifNullThrow(),
            cursorPaginationRequestDto
        )
    }

    /**
     * 정렬 기준이 ID 가 아닌 모든 상황에서 WHERE 절의 커서 검색 조건을 만듦
     *
     * @param [path] 정렬의 기준이 되는 필드 (id 필드값이 [SortField] 에서 null 로 표현되긴 하지만 id가 정렬 기준인 경우엔 선처리를 해줘야함 [BaseSortField] 참고)
     * @param [idPath] 엔티티 id(정렬 기준과는 관계없이 필수)
     * @param [parseCursor] [CursorPaginationRequestDto.cursorValue]을 sortField 에 열거된 필드에 일치하는 타입으로 변환하는 람다.
     * 만약, 람다에서 변환할 수 없는 경우 null을 반환해야함
     */
    fun <T : Comparable<*>> createFieldCursorCondition(
        path: ComparableExpressionBase<T>,
        idPath: NumberPath<Long>,
        cursorPaginationRequestDto: CursorPaginationRequestDto,
        parseCursor: (String) -> T?
    ): BooleanExpression? {
        val idCondition = createIdCursorCondition(idPath, cursorPaginationRequestDto) ?: return null
        // 이전 페이지의 마지막 항목도 정렬값이 null 이면 null 인 항목 안에서 ID 로 이어서 조회함
        val cursorValue = cursorPaginationRequestDto.cursorValue ?: return path.isNull.and(idCondition)
        val value = parseCursor(cursorValue) ?: throw BusinessException(CommonError.INVALID_INPUT_VALUE)

        /**
         * CREATED_AT + ASC 인 경우 SQL 예시
         *
         * created_at > cursorValue OR ( created_at = cursorValue AND id > cursorId ) OR created_at IS NULL
         *
         * nullsLast 정렬을 하면 .or(path.isNull) 를 넣어야만 해당 필드가 null 이더라도 가져옴.
         *
         * created_at은 not null 조건이 붙어있어서 필수는 아니지만 예를들어 description 같은게 nullable 이면 이 조건이 꼭 있어야 다음페이지에 description 이 null 로 시작하더라도 포함됨.
         */
        return createComparisonBySortDirection(path, value, cursorPaginationRequestDto)
            .or(path.eq(value).and(idCondition))
            .or(path.isNull)
    }

    /**
     * ORDER BY 정렬 기준을 생성하는 메서드
     *
     * asc desc 상관없이 정렬할 필드값이 null 인 항목은 마지막으로 위치 시킴
     *
     * @return ID 자체로 정렬하는 경우엔 같은 정렬 조건을 두번 넣을 필요가 없으므로 ID 정렬만 반환
     */
    fun createOrderSpecifiers(
        path: ComparableExpressionBase<*>,
        idPath: NumberPath<Long>,
        cursorPaginationRequestDto: CursorPaginationRequestDto
    ): Array<OrderSpecifier<*>> {
        val order = if (cursorPaginationRequestDto.isSortDirectionAsc()) {
            Order.ASC
        } else {
            Order.DESC
        }
        val idOrder = OrderSpecifier(order, idPath)
        if (path == idPath) {
            return arrayOf(idOrder)
        }
        return arrayOf(OrderSpecifier(order, path).nullsLast(), idOrder)
    }

    /**
     * WHERE 절의 커서 검색 조건을 만드는 메서드
     *
     * 더 구체적으론 [org.springframework.data.domain.Sort.Direction] 에 따라 [path] 와 [value] 간의 비교 조건식을 다르게 만드는 메서드
     *
     * 날짜, 문자열, 숫자 필드를 같은 메서드에서 비교하기 위해 [ComparableExpressionBase] 로 [path]를 받음
     *
     * 이 타입에는 gt, lt 가 없어서 [Ops.GT], [Ops.LT] 로 비교식을 만듦
     */
    private fun <T : Comparable<*>> createComparisonBySortDirection(
        path: ComparableExpressionBase<T>,
        value: T,
        cursorPaginationRequestDto: CursorPaginationRequestDto
    ): BooleanExpression {
        val operator = if (cursorPaginationRequestDto.isSortDirectionAsc()) Ops.GT else Ops.LT
        return Expressions.booleanOperation(operator, path, Expressions.constant(value))
    }
}
