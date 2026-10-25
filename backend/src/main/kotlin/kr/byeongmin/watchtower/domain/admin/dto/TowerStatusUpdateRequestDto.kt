package kr.byeongmin.watchtower.domain.admin.dto

import kr.byeongmin.watchtower.domain.tower.enums.TowerStatus

data class TowerStatusUpdateRequestDto(
    val status: TowerStatus
)
