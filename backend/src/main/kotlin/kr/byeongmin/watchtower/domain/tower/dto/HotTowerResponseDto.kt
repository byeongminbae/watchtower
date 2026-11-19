package kr.byeongmin.watchtower.domain.tower.dto

import kr.byeongmin.watchtower.domain.tower.entity.HotTower

/**
 * @property [domain] 예시: https://map.naver.com/p?c=16.00,0,0,0,dh -> map.naver.com
 * @property [url] 예시: https://map.naver.com/p?c=16.00,0,0,0,dh -> https://map.naver.com
 */
data class HotTowerResponseDto(
    val domain: String,
    val url: String,
    val detectionCount: Long
) {
    companion object {
        fun from(hotTower: HotTower): HotTowerResponseDto {
            return HotTowerResponseDto(
                domain = hotTower.domain,
                url = hotTower.url,
                detectionCount = hotTower.detectionCount
            )
        }
    }
}