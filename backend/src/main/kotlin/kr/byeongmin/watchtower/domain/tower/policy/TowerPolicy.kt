package kr.byeongmin.watchtower.domain.tower.policy

import java.time.Period

object TowerPolicy {
    val SNAPSHOT_RETENTION_PERIOD: Period = Period.ofMonths(1)
    const val MIN_FETCH_INTERVAL_MINUTES = 5L
    const val MAX_FETCH_INTERVAL_HOURS = 24L
    const val FETCH_INTERVAL_STEP_SECONDS = 300L
}
