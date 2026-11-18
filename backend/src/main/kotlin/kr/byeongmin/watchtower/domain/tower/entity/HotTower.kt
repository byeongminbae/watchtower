package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Subselect

@Entity
@Immutable
@Subselect("SELECT domain, url, detection_count FROM hot_tower")
class HotTower(
    @Id
    val domain: String,
    val url: String,
    val detectionCount: Long
)
