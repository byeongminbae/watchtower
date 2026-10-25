package kr.byeongmin.watchtower.global.repository

import kr.byeongmin.watchtower.global.entity.HistoryBase
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import kotlin.jvm.optionals.getOrNull

/** [HistoryBase] 엔티티를 상속받은 엔티티에 대한 레포지토리는 이 인터페이스를 구현해야함  */
@NoRepositoryBean
interface HistoryBaseRepository<T : HistoryBase, ID : Any> : JpaRepository<T, ID> {
    fun find(id: ID): T? {
        return findById(id).getOrNull()
    }

    fun get(id: ID): T {
        return find(id) ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)
    }
}
