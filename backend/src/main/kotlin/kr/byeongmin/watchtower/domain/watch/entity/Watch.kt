package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.watch.enums.WatchStatus
import kr.byeongmin.watchtower.global.entity.Base
import kr.byeongmin.watchtower.global.utils.TimeUtil
import java.time.LocalDateTime

@Entity
class Watch(
    @ManyToOne(fetch = FetchType.LAZY)
    private val createdBy: Member,
    private val name: String,
    private val url: String,
    private val intervalSeconds: Int,
    private val includeInStat: Boolean,
    @Enumerated(EnumType.STRING)
    private val status: WatchStatus
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