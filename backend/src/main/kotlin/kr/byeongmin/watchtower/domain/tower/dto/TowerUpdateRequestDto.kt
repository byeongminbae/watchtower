package kr.byeongmin.watchtower.domain.tower.dto

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Duration
import kr.byeongmin.watchtower.domain.tower.enums.UpdatableTowerStatus
import kr.byeongmin.watchtower.domain.tower.policy.TowerPolicy
import kr.byeongmin.watchtower.global.validation.DurationMultipleOf
import org.hibernate.validator.constraints.time.DurationMax
import org.hibernate.validator.constraints.time.DurationMin

data class TowerUpdateRequestDto(
    @field:NotBlank
    @field:Size(max = 60)
    val name: String,
    @field:DurationMin(
        minutes = TowerPolicy.MIN_FETCH_INTERVAL_MINUTES,
        message = "확인 주기는 {minutes}분 이상이어야 합니다."
    )
    @field:DurationMax(
        hours = TowerPolicy.MAX_FETCH_INTERVAL_HOURS,
        message = "확인 주기는 {hours}시간 이하여야 합니다."
    )
    @field:DurationMultipleOf(seconds = TowerPolicy.FETCH_INTERVAL_STEP_SECONDS)
    @field:JsonFormat(shape = JsonFormat.Shape.STRING)
    val fetchInterval: Duration,
    val includeInStat: Boolean,
    val status: UpdatableTowerStatus
)
