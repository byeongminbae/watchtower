package kr.byeongmin.watchtower.domain.tower.repository

import kr.byeongmin.watchtower.global.repository.BaseRepository
import kr.byeongmin.watchtower.domain.tower.entity.TowerSnapshot

interface TowerSnapshotRepository : BaseRepository<TowerSnapshot, Long>
