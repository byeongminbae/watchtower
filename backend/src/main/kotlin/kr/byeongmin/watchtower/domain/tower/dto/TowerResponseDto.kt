package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Duration
import java.time.LocalDateTime
import kr.byeongmin.watchtower.domain.tower.entity.Tower
import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus
import kr.byeongmin.watchtower.global.utils.ifNullThrow

data class TowerResponseDto(
    val id: Long,
    val name: String,
    val url: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val fetchInterval: Duration,
    val includeInStat: Boolean,
    val status: TowerStatus,
    val faviconUrl: String?,
    val lastFetchedAt: LocalDateTime?,
    val lastNotifiedAt: LocalDateTime?
) {
    companion object {
        fun from(tower: Tower): TowerResponseDto {
            return TowerResponseDto(
                id = tower.id.ifNullThrow(),
                name = tower.name,
                url = tower.url,
                fetchInterval = tower.fetchInterval,
                includeInStat = tower.includeInStat,
                status = tower.status,
                faviconUrl = tower.faviconUrl,
                lastFetchedAt = tower.lastFetchedAt,
                lastNotifiedAt = tower.lastNotifiedAt
            )
        }
    }
}
