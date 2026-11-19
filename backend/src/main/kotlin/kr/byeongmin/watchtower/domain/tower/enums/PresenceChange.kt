package kr.byeongmin.watchtower.domain.tower.enums

enum class PresenceChange {
    APPEAR,
    DISAPPEAR;

    fun hasOccurred(previousCount: Int, currentCount: Int): Boolean {
        return when (this) {
            APPEAR -> currentCount > previousCount
            DISAPPEAR -> currentCount < previousCount
        }
    }
}
