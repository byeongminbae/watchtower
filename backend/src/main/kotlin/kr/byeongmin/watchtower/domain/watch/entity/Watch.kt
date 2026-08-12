package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.watch.enums.WatchStatus
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

@Entity
class Watch private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    val createdBy: Member,
    val name: String,
    val url: String,
    val intervalSeconds: Int,
    val includeInStat: Boolean,
    @Enumerated(EnumType.STRING)
    val status: WatchStatus
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "watch_seq"
    )
    @SequenceGenerator(
        name = "watch_seq",
        sequenceName = "watch_id_seq",
        allocationSize = 30
    )
    val id: Long? = null

    private var faviconUrl: String? = null
    private var lastFetchedAt: LocalDateTime? = null
    private var lastNotifiedAt: LocalDateTime? = null

    fun updateFaviconUrl(faviconUrl: String) {
        this.faviconUrl = faviconUrl
    }

    fun fetched() {
        this.lastFetchedAt = TimeUtil.entityTime()
    }

    fun notified() {
        this.lastNotifiedAt = TimeUtil.entityTime()
    }
}