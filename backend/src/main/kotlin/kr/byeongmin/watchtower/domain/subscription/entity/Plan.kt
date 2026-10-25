package kr.byeongmin.watchtower.domain.subscription.entity

import jakarta.persistence.*
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.global.entity.Base
import java.time.LocalDateTime

// TODO: 어드민 콘솔 고도화 하면서 플랜 crud 도 만들어야할듯. 크게 중요한거 아니니 나중에 할 예정
/**
 * [kr.byeongmin.watchtower.domain.member.entity.Member]가 이용하는 플랜
 *
 * 결제와는 무관하게 회원별 리소스 이용 한도(타워/조건 개수)만 정의한다.
 *
 * @property [type] 관리자가 아닌 회원은 [PlanType.FREE] 플랜을 가짐
 * @property [maxTowerQuantity] 회원이 등록할 수 있는 타워 최대 개수
 * @property [maxConditionQuantity] 타워 하나에 등록할 수 있는 조건 최대 개수
 * @property [saleStartedAt] 플랜 판매 시작 시각
 * @property [saleEndedAt] 플랜 판매 종료 시각. null 이면 무기한 플랜임
 */
@Entity
class Plan private constructor(
    name: String,
    type: PlanType,
    maxTowerQuantity: Int,
    maxConditionQuantity: Int,
    saleStartedAt: LocalDateTime,
    saleEndedAt: LocalDateTime? = null
) : Base() {
    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "plan_seq"
    )
    @SequenceGenerator(
        name = "plan_seq",
        sequenceName = "plan_id_seq",
        allocationSize = 30
    )
    override val id: Long? = null

    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    var type: PlanType = type
        protected set

    var maxTowerQuantity: Int = maxTowerQuantity
        protected set

    var maxConditionQuantity: Int = maxConditionQuantity
        protected set

    var saleStartedAt: LocalDateTime = saleStartedAt
        protected set

    var saleEndedAt: LocalDateTime? = saleEndedAt
        protected set

    fun changeName(name: String) {
        this.name = name
    }

    fun changeType(type: PlanType) {
        this.type = type
    }

    fun changeMaxTowerQuantity(maxTowerQuantity: Int) {
        this.maxTowerQuantity = maxTowerQuantity
    }

    fun changeMaxConditionQuantity(maxConditionQuantity: Int) {
        this.maxConditionQuantity = maxConditionQuantity
    }

    fun changeSaleStartedAt(saleStartedAt: LocalDateTime) {
        this.saleStartedAt = saleStartedAt
    }

    fun changeSaleEndedAt(saleEndedAt: LocalDateTime) {
        this.saleEndedAt = saleEndedAt
    }

    fun makeSaleEndless() {
        this.saleEndedAt = null
    }
}
