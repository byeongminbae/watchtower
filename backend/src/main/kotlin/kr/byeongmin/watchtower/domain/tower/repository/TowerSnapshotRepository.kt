package kr.byeongmin.watchtower.domain.tower.repository

import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface TowerSnapshotRepository : BaseRepository<TowerSnapshot, Long> {
    fun findByIdAndTowerAndDeletedAtIsNull(id: Long, tower: Tower): TowerSnapshot?
}
