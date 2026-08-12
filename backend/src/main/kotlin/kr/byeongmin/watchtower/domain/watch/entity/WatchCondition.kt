package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.global.entity.Base

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn
abstract class WatchCondition(
    @ManyToOne(fetch = FetchType.LAZY)
    protected val watch: Watch
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "watch_condition_seq"
    )
    @SequenceGenerator(
        name = "watch_condition_seq",
        sequenceName = "watch_condition_id_seq",
        allocationSize = 30
    )
    val id: Long? = null
}