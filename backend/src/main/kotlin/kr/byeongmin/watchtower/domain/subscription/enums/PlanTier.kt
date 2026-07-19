package kr.byeongmin.watchtower.domain.subscription.enums

enum class PlanTier(private val level: Int) {
    FREE(0), BASIC(10), PRO(100)
}