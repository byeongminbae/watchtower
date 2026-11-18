package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonFormat
import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

/**
 * 크롤러 작업에 대한 결과(필드는 crawler 의 CrawlResultMessage 와 같아야 함)
 *
 * @property [requestedUrl] 크롤링을 요청한 URL. 요청 메시지의 값을 크롤러가 그대로 돌려준 것이라 리다이렉트된 최종 URL 이 아님
 * @property [finalUrl] 리다이렉트를 모두 따라간 뒤 페이지를 응답한 URL. 페이지를 받아오지 못했으면 null
 * @property [jobStartedAt] 백엔드가 이 크롤링 작업을 시작한 시각. 요청 메시지의 값을 크롤러가 그대로 돌려준 것임
 * @property [crawlerResultCode] 크롤러가 판정한 크롤링 결과. 4xx, 5xx 응답도 페이지를 받아온 것이라 [CrawlerResultCode.FETCHED], 실패하면 crawler 의 CrawlFailureCode 값. 백엔드가 모르는 값은 [CrawlerResultCode.UNKNOWN]
 */
data class CrawlResultMessageDto(
    val towerId: Long,
    val requestedUrl: String,
    val finalUrl: String?,
    val jobStartedAt: LocalDateTime,
    val crawlingStartedAt: LocalDateTime,
    val crawlingEndedAt: LocalDateTime,
    val triggeredByMemberId: Long?,
    @JsonFormat(with = [JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE])
    val crawlerResultCode: CrawlerResultCode,
    val pageHttpStatusCode: Int?,
    val pageContent: PageContent?
) {
    fun isCrawlingSucceed(): Boolean {
        return crawlerResultCode == CrawlerResultCode.FETCHED &&
                pageHttpStatusCode?.let { HttpStatus.Series.resolve(it) } == HttpStatus.Series.SUCCESSFUL
    }

    fun hasUnknownResultCode(): Boolean {
        return crawlerResultCode == CrawlerResultCode.UNKNOWN
    }

    /**
     * 크롤러가 가져온 데이터에 대한 정보
     *
     * 현재 해시는 SHA-256을 공통으로 사용함
     * @property [dateTimeIgnoredTextHash] 날짜 무시 설정과 상관없이 크롤러가 항상 계산함
     * @property [detectionAreaSelector] 백엔드에서 보낸값 그대로 반환
     * @property [textObjectName] 텍스트와 이미지가 모두 직전 버전과 같아서 크롤러가 저장하지 않았으면 null
     * @property [imageHash] 이미지 해시 목록을 정렬해 합친 해시. 이미지 변화 감지를 끈 타워면 null
     */
    data class PageContent(
        val textHash: String,
        val dateTimeIgnoredTextHash: String,
        val detectionAreaSelector: String?,
        val textObjectName: String?,
        val imageHash: String?
    ) {
        /** @param [textLoader] 텍스트가 필요한 조건이 읽을 때만 이번 버전의 텍스트 객체를 읽는 함수 */
        fun to(ignoreDateTime: Boolean, textLoader: () -> String): DetectionContent {
            return DetectionContent(
                textHash = selectTextHash(ignoreDateTime),
                detectionAreaSelector = detectionAreaSelector,
                imageHash = imageHash,
                textLoader = textLoader
            )
        }

        private fun selectTextHash(ignoreDateTime: Boolean): String {
            if (ignoreDateTime) {
                return dateTimeIgnoredTextHash
            }
            return textHash
        }
    }
}
