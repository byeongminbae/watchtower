package kr.byeongmin.watchtower.domain.tower.repository

import kr.byeongmin.watchtower.global.repository.HistoryBaseRepository
import kr.byeongmin.watchtower.domain.tower.entity.TowerRequestHistory

interface TowerRequestHistoryRepository : HistoryBaseRepository<TowerRequestHistory, Long>
