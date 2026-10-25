package kr.byeongmin.watchtower.domain.member.policy

import java.time.Period

object MemberPolicy {
    val INACTIVITY_PERIOD: Period = Period.ofYears(1)
}
