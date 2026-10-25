package kr.byeongmin.watchtower.domain.tower.dto

/**
 * @property [domain] 예시: https://map.naver.com/p?c=16.00,0,0,0,dh -> map.naver.com
 * @property [url] 예시: https://map.naver.com/p?c=16.00,0,0,0,dh -> https://map.naver.com
 */
// TODO: changeCount 는 더 명확하게 changeDetectedCount 로 명확하게 가는게 나을듯 근데 감지 횟수는 동의와는 별개로 타워에서 카운트 해야할거같은데?
data class HotTowerResponseDto(
    val domain: String,
    val url: String,
    val changeCount: Long
)