package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.TowerRequestHistory
import kr.byeongmin.watchtower.domain.tower.repository.TowerRequestHistoryRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TowerRequestHistoryAccessor(
    private val towerRequestHistoryRepository: TowerRequestHistoryRepository
) {
    fun save(towerRequestHistory: TowerRequestHistory): TowerRequestHistory {
        return towerRequestHistoryRepository.save(towerRequestHistory)
    }

    fun isCrawlResultApplied(towerId: Long, crawlingStartedAt: LocalDateTime): Boolean {
        return towerRequestHistoryRepository.existsByTower_IdAndCrawlingStartedAt(towerId, crawlingStartedAt)
    }
}
