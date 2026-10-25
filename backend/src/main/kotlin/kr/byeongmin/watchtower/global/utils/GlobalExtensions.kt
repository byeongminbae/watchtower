package kr.byeongmin.watchtower.global.utils

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import java.math.BigDecimal
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 조건상 null 이 아니어야 하지만 nullable 타입으로 선언된 값에 사용함
 *
 * 다만 null 이 정상적인 상황에 발생할 수 있는 값이라면 사용하지 않음
 *
 * 논리적으로 null safety가 보장되는 경우에만 사용할것
 *
 * 예: 영속화되어 ID가 할당된 엔티티의 id
 */
fun <T> T?.ifNullThrow(): T {
    if (this.isNull()) throw BusinessException(CommonError.NULL_CASTING_ERROR)
    return this
}

@OptIn(ExperimentalContracts::class)
fun <T> T?.isNull(): Boolean {
    contract {
        returns(true) implies (this@isNull == null)
        returns(false) implies (this@isNull != null)
    }
    return this == null
}

@OptIn(ExperimentalContracts::class)
fun <T> T?.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
        returns(false) implies (this@isNotNull == null)
    }
    return this != null
}

fun BigDecimal.isZero(): Boolean {
    return this.compareTo(BigDecimal.ZERO) == 0
}
