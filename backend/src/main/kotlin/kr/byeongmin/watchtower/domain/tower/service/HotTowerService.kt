package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.tower.accessor.HotTowerAccessor
import kr.byeongmin.watchtower.domain.tower.dto.HotTowerResponseDto
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class HotTowerService(
    private val hotTowerAccessor: HotTowerAccessor
) {
    fun getTrendingTowers(): SuccessDataResponse<List<HotTowerResponseDto>> {
        return SuccessDataResponse(hotTowerAccessor.findHotTowers().map { HotTowerResponseDto.from(it) })
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    fun refreshHotTowers() {
        hotTowerAccessor.refreshHotTowers()
    }
}
