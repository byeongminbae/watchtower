package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.entity.HotTower
import kr.byeongmin.watchtower.domain.tower.repository.HotTowerRepository
import org.springframework.stereotype.Component

@Component
class HotTowerAccessor(
    private val hotTowerRepository: HotTowerRepository
) {
    fun findHotTowers(): List<HotTower> {
        return hotTowerRepository.findAll()
    }

    fun refreshHotTowers() {
        hotTowerRepository.refresh()
    }
}
