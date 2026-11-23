package kr.byeongmin.watchtower.external.discord.dto

data class DiscordErrorResponseDto(
    val code: Int
) {
    fun isDirectMessageBlocked(): Boolean {
        return code == DIRECT_MESSAGE_BLOCKED_CODE
    }

    companion object {
        const val DIRECT_MESSAGE_BLOCKED_CODE = 50007
    }
}
