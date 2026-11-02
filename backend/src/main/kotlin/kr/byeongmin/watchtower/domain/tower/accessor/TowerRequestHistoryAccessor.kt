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

    /** 같은 크롤링 결과는 크롤링 시작 시각도 같아서, 이미 있으면 그 결과를 이미 반영한 것임 */
    fun existsByTowerIdAndStartedAt(towerId: Long, startedAt: LocalDateTime): Boolean {
        return towerRequestHistoryRepository.existsByTower_IdAndStartedAt(towerId, startedAt)
    }
}
