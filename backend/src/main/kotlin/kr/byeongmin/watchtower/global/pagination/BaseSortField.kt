package kr.byeongmin.watchtower.global.pagination

import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.DateTimePath
import com.querydsl.core.types.dsl.NumberPath
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.entity.QBase
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * 커서 페이지네이션을 수행할 때 어떤 필드를 기준으로 정렬할 것인지 열거한 클래스
 *
 * [BaseSortField] 는 [kr.byeongmin.watchtower.global.entity.Base] 에 존재하는 필드로 구성된 정렬 필드 클래스고
 * 다른 엔티티에 존재하는 필드를 기준으로 정렬하고 싶은 경우엔 SortField 의 구현 enum 을 해당 레이어에서 만들면 됨
 *
 * [ID] 는 [QBase] 에 ID 필드가 없으므로 null 을 id 필드로 규정함
 */
enum class BaseSortField(
    private val field: ((QBase) -> DateTimePath<LocalDateTime>)?
) : SortField {
    // TODO: 널로 특정 필드를 구분 하는것이 좀 그렇긴 하다. 다른 사람이 확장하다가 오해하진 않을까.. 이게 최선인지 고민해보자
    ID(null),
    CREATED_AT({ it.createdAt }),
    UPDATED_AT({ it.updatedAt });

    override fun to(direction: Sort.Direction): Sort {
        return Sort.by(direction, field?.invoke(QBase.base)?.metadata?.name ?: "id")
    }

    override fun getCursorValue(entity: Base): String? {
        return when (this) {
            ID -> null
            CREATED_AT -> entity.createdAt?.toString()
            UPDATED_AT -> entity.updatedAt?.toString()
        }
    }

    fun getCursorCondition(
        entity: QBase,
        idPath: NumberPath<Long>,
        cursorPaginationRequestDto: CursorPaginationRequestDto
    ): BooleanExpression? {
        val path = field?.invoke(entity)
            ?: return CursorQuery.createIdCursorCondition(idPath, cursorPaginationRequestDto)
        return CursorQuery.createFieldCursorCondition(
            path,
            idPath,
            cursorPaginationRequestDto,
            ::parseCursorValue
        )
    }

    fun getOrderSpecifiers(
        base: QBase,
        id: NumberPath<Long>,
        request: CursorPaginationRequestDto
    ): Array<OrderSpecifier<*>> {
        val path = field?.invoke(base) ?: id
        return CursorQuery.createOrderSpecifiers(path, id, request)
    }

    private fun parseCursorValue(value: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
