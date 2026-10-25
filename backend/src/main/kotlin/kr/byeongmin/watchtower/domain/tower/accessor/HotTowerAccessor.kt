package kr.byeongmin.watchtower.domain.tower.accessor

import kr.byeongmin.watchtower.domain.tower.dto.HotTowerResponseDto
import kr.byeongmin.watchtower.domain.tower.repository.HotTowerRepository
import org.springframework.stereotype.Component

@Component
class HotTowerAccessor(
    private val hotTowerRepository: HotTowerRepository
) {
    fun findAll(): List<HotTowerResponseDto> {
        return hotTowerRepository.findAll()
    }

    fun refresh() {
        hotTowerRepository.refresh()
    }
}
