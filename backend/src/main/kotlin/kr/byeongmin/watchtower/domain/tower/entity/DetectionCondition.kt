package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.tower.vo.DetectionContent
import kr.byeongmin.watchtower.global.entity.Base

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn
abstract class DetectionCondition(
    @ManyToOne(fetch = FetchType.LAZY)
    protected val tower: Tower
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "tower_condition_seq"
    )
    @SequenceGenerator(
        name = "tower_condition_seq",
        sequenceName = "tower_condition_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    abstract fun isConditionSatisfied(previous: DetectionContent, current: DetectionContent): Boolean
}