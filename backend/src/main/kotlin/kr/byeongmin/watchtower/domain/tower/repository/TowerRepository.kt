package kr.byeongmin.watchtower.domain.tower.repository

import jakarta.persistence.LockModeType
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.repository.BaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock

interface TowerRepository : BaseRepository<Tower, Long> {
    fun findByIdAndCreatedBy_IdAndDeletedAtIsNull(id: Long, memberId: Long): Tower?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findTowerForUpdateByIdAndCreatedBy_IdAndDeletedAtIsNull(id: Long, memberId: Long): Tower?

    fun countByCreatedBy_IdAndDeletedAtIsNull(memberId: Long): Long

    fun countByStatusAndDeletedAtIsNull(status: TowerStatus): Long

    fun findByNameStartingWith(name: String, pageable: Pageable): Page<Tower>

}
