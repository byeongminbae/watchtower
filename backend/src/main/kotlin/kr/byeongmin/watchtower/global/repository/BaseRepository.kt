package kr.byeongmin.watchtower.global.repository

import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import kotlin.jvm.optionals.getOrNull

/** [Base] 엔티티를 상속받은 엔티티에 대한 레포지토리는 이 인터페이스를 구현해야함  */
@NoRepositoryBean
interface BaseRepository<T : Base, ID : Any> : JpaRepository<T, ID> {
    @Query("select e from #{#entityName} e where e.id = :id and e.deletedAt is null")
    fun find(@Param("id") id: ID): T?

    fun get(id: ID, func: () -> Nothing): T {
        return find(id) ?: func()
    }

    fun get(id: ID): T {
        return get(id) { throw BusinessException(CommonError.RESOURCE_NOT_FOUND) }
    }

    fun findAny(id: ID): T? {
        return findById(id).getOrNull()
    }

    fun getAny(id: ID, func: () -> Nothing): T {
        return findAny(id) ?: func()
    }

    fun getAny(id: ID): T {
        return getAny(id) { throw BusinessException(CommonError.RESOURCE_NOT_FOUND) }
    }
}
