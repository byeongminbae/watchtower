package kr.byeongmin.watchtower.external.discord.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class DiscordUserResponseDto(
    val id: String,
    val username: String,
    @JsonProperty("global_name")
    val globalName: String?,
    val avatar: String?
) {
    fun getDisplayName(): String {
        return globalName ?: username
    }

    fun getAvatarUrl(): String {
        if (avatar == null) {
            // https://docs.discord.com/developers/reference#:~:text=index%20will%20be-,(user_id%20%3E%3E%2022)%20%25%206,-.%20For%20users%20on
            return "$CDN_BASE_URL/embed/avatars/${(id.toULong() shr 22) % DEFAULT_AVATAR_COUNT}.png"
        }
        return "$CDN_BASE_URL/avatars/$id/$avatar.png"
    }

    companion object {
        const val CDN_BASE_URL = "https://cdn.discordapp.com"
        const val DEFAULT_AVATAR_COUNT = 6UL
    }
}
