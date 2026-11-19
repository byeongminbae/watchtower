package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Duration
import java.time.LocalDateTime
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.CrawlerResultCode
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.utils.ifNullThrow

data class TowerResponseDto(
    val id: Long,
    val name: String,
    val url: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val fetchInterval: Duration,
    val includeInStat: Boolean,
    val detectTextChange: Boolean,
    val detectImageChange: Boolean,
    val ignoreDateTime: Boolean,
    val detectionAreaSelector: String?,
    val status: TowerStatus,
    val faviconUrl: String?,
    val lastFetchedAt: LocalDateTime?,
    val lastCrawlerResultCode: CrawlerResultCode?,
    val lastNotifiedAt: LocalDateTime?,
    val latestAiSummary: String?
) {
    companion object {
        fun from(tower: Tower, latestAiSummary: String?): TowerResponseDto {
            return TowerResponseDto(
                id = tower.id.ifNullThrow(),
                name = tower.name,
                url = tower.url,
                fetchInterval = tower.fetchInterval,
                includeInStat = tower.includeInStat,
                detectTextChange = tower.detectTextChange,
                detectImageChange = tower.detectImageChange,
                ignoreDateTime = tower.ignoreDateTime,
                detectionAreaSelector = tower.detectionAreaSelector,
                status = tower.status,
                faviconUrl = tower.faviconUrl,
                lastFetchedAt = tower.lastFetchedAt,
                lastCrawlerResultCode = tower.lastCrawlerResultCode,
                lastNotifiedAt = tower.lastNotifiedAt,
                latestAiSummary = latestAiSummary
            )
        }
    }
}
