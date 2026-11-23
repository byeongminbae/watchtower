package kr.byeongmin.watchtower.external.discord.client

import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.external.discord.dto.DiscordChannelResponseDto
import kr.byeongmin.watchtower.external.discord.dto.DiscordErrorResponseDto
import kr.byeongmin.watchtower.external.discord.dto.DiscordUserResponseDto
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.USER_AGENT
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import tools.jackson.databind.ObjectMapper

@Component
class DiscordClient(
    restClient: RestClient,
    private val objectMapper: ObjectMapper,
    @Value("\${discord-application-token}") botToken: String,
    @Value("\${watchtower.discord.guild-id}") private val guildId: String
) {
    private val discordRestClient = restClient.mutate()
        .baseUrl(API_BASE_URL)
        .defaultHeader(USER_AGENT, USER_AGENT_VALUE)
        .build()
    private val botAuthorization = "Bot $botToken"

    fun fetchUser(accessToken: String): DiscordUserResponseDto {
        return discordRestClient.get()
            .uri("/users/@me")
            .header(AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body<DiscordUserResponseDto>()
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }

    fun addGuildMember(userId: String, accessToken: String) {
        discordRestClient.put()
            .uri("/guilds/{guildId}/members/{userId}", guildId, userId)
            .header(AUTHORIZATION, botAuthorization)
            .contentType(APPLICATION_JSON)
            .body(mapOf("access_token" to accessToken))
            .retrieve()
            .toBodilessEntity()
    }

    fun createDirectMessageChannel(userId: String): String {
        return discordRestClient.post()
            .uri("/users/@me/channels")
            .header(AUTHORIZATION, botAuthorization)
            .contentType(APPLICATION_JSON)
            .body(mapOf("recipient_id" to userId))
            .retrieve()
            .body<DiscordChannelResponseDto>()
            ?.id
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }

    fun sendMessage(channelId: String, content: String) {
        discordRestClient.post()
            .uri("/channels/{channelId}/messages", channelId)
            .header(AUTHORIZATION, botAuthorization)
            .contentType(APPLICATION_JSON)
            .body(mapOf("content" to content, "allowed_mentions" to mapOf("parse" to emptyList<String>())))
            .retrieve()
            .onStatus({ it == HttpStatus.FORBIDDEN }) { _, response -> throw toMessageRejectedException(response) }
            .toBodilessEntity()
    }

    private fun toMessageRejectedException(response: ClientHttpResponse): BusinessException {
        val discordError = objectMapper.readValue(response.body, DiscordErrorResponseDto::class.java)
        if (discordError.isDirectMessageBlocked()) {
            return BusinessException(SocialAccountError.DISCORD_DIRECT_MESSAGE_BLOCKED)
        }
        return BusinessException(CommonError.EXTERNAL_API_ERROR)
    }

    companion object {
        const val API_BASE_URL = "https://discord.com/api/v10"
        const val USER_AGENT_VALUE = "DiscordBot (https://watchtower.boo, 1.0)"
    }
}
