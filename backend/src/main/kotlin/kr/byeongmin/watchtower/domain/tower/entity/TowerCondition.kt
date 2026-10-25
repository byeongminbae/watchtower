package kr.byeongmin.watchtower.domain.tower.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.global.entity.Base

/**
 * 타워에 등록된 알림 조건의 기준
 *
 * 추가는 마음대로지만 삭제는 아님. 지원할 조건은 신중히 추가해야함
 *
 * 유저가 등록해놓은 조건을 코드상에서 지워버리면 [kr.byeongmin.watchtower.domain.tower.dto.TowerConditionResponseDto] 에서 BusinessException 이 발생함
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn
abstract class TowerCondition(
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

    /** @return 이전 HTML과 현재 HTML를 구현체에 맞는 조건을 충족하는지 여부 */
    abstract fun isSatisfied(previousHtml: String, currentHtml: String): Boolean
}