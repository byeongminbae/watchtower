package kr.byeongmin.watchtower.global.pagination

import kr.byeongmin.watchtower.global.entity.Base
import org.springframework.data.domain.Sort

interface SortField {
    /**
     * 오프셋 페이지네이션 정렬에 필요한 [Sort] (필드+방향) 객체로 변환
     */
    fun to(direction: Sort.Direction): Sort

    /**
     * 커서 페이지네이션 hasNext 가 true 일떄 반환된 마지막 엔티티를 기준으로 [CursorPaginationRequestDto.cursorValue] 를 [String] 으로 변환하여 반환
     */
    fun getCursorValue(entity: Base): String?
}
