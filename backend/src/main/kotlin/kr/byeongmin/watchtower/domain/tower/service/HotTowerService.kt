package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.tower.dto.HotTowerResponseDto
import kr.byeongmin.watchtower.domain.tower.accessor.HotTowerAccessor
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import org.springframework.stereotype.Service

@Service
class HotTowerService(
    private val hotTowerAccessor: HotTowerAccessor
) {
    /** @return 마지막 뷰 갱신 시점의 인기 도메인 집계 결과 */
    fun getTrendingTowers(): SuccessDataResponse<List<HotTowerResponseDto>> {
        return SuccessDataResponse(hotTowerAccessor.findAll())
    }

    // TODO: 스케줄러가 01:00 에 호출
    fun refreshHotTowers() {
        hotTowerAccessor.refresh()
    }
}
