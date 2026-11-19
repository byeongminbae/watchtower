package kr.byeongmin.watchtower.domain.tower.dto

/**
 * 결과 큐에서 받은 메시지
 *
 * @property [receipt] 반영한 메시지를 큐에서 지울 때 쓰는 값
 * @property [content] [CrawlResultMessageDto] 의 JSON
 */
data class QueueResultMessageDto(
    val receipt: String,
    val content: String
)
