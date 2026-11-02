package kr.byeongmin.watchtower.domain.tower.repository

import kr.byeongmin.watchtower.global.repository.HistoryBaseRepository
import kr.byeongmin.watchtower.domain.tower.entity.TowerRequestHistory
import java.time.LocalDateTime

interface TowerRequestHistoryRepository : HistoryBaseRepository<TowerRequestHistory, Long> {
    fun existsByTower_IdAndStartedAt(towerId: Long, startedAt: LocalDateTime): Boolean
}
