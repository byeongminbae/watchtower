package kr.byeongmin.watchtower.domain.tower.enums

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

/**
 * @property [UNKNOWN] 크롤러와 백엔드간의 enum이 불일치 하는경우 기본값
 */
enum class CrawlerResultCode {
    FETCHED,
    URL_NOT_ALLOWED,
    NAVIGATION_TIMEOUT,
    TEXT_TOO_LARGE,
    NAVIGATION_FAILED,
    INVALID_SELECTOR,

    @JsonEnumDefaultValue
    UNKNOWN
}
