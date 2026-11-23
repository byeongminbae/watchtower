package kr.byeongmin.watchtower.external.discord

import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DiscordClientTest {
    private val objectMapper = jacksonObjectMapper()
    private val botToken = "discord-application-token"
    private val guildId = "1573265772178968628"
    private val userId = "80351110224678912"
    private val accessToken = "Discord-가-발급한-회원-액세스-토큰"
    private val directMessageChannelId = "1573265772178968700"

    private val restClientBuilder = RestClient.builder()
    private val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val discordClient = DiscordClient(
        restClient = restClientBuilder.build(),
        objectMapper = objectMapper,
        botToken = botToken,
        guildId = guildId
    )

    @Test
    fun `회원 액세스 토큰으로 Discord 사용자 ID 와 이름을 가져옴`() {
        /** given */
        mockServer
            .expect(requestTo("https://discord.com/api/v10/users/@me"))
            .andExpect(method(GET))
            .andExpect(header(AUTHORIZATION, "Bearer $accessToken"))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsBytes(
                        mapOf("id" to userId, "username" to "nelly", "global_name" to null, "avatar" to null)
                    ),
                    APPLICATION_JSON
                )
            )

        /** when */
        val result = discordClient.fetchUser(accessToken)

        /** then */
        assertEquals(userId, result.id)
        assertEquals("nelly", result.getDisplayName())
        mockServer.verify()
    }

    @Test
    fun `봇 토큰과 회원 액세스 토큰으로 회원을 우리 서버에 가입시킴`() {
        /** given */
        mockServer
            .expect(requestTo("https://discord.com/api/v10/guilds/$guildId/members/$userId"))
            .andExpect(method(PUT))
            .andExpect(header(AUTHORIZATION, "Bot $botToken"))
            .andExpect(content().json(objectMapper.writeValueAsString(mapOf("access_token" to accessToken))))
            .andRespond(withNoContent())

        /** when */
        discordClient.addGuildMember(userId, accessToken)

        /** then */
        mockServer.verify()
    }

    @Test
    fun `봇이 회원과의 DM 채널을 열면 DM 채널 ID 를 반환함`() {
        /** given */
        mockServer
            .expect(requestTo("https://discord.com/api/v10/users/@me/channels"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, "Bot $botToken"))
            .andExpect(content().json(objectMapper.writeValueAsString(mapOf("recipient_id" to userId))))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsBytes(mapOf("id" to directMessageChannelId, "type" to 1)),
                    APPLICATION_JSON
                )
            )

        /** when */
        val result = discordClient.createDirectMessageChannel(userId)

        /** then */
        assertEquals(directMessageChannelId, result)
        mockServer.verify()
    }

    @Test
    fun `메시지는 멘션 알림 없이 DM 채널로 보냄`() {
        /** given */
        val message = "@everyone 이 들어간 페이지 변화"
        mockServer
            .expect(requestTo("https://discord.com/api/v10/channels/$directMessageChannelId/messages"))
            .andExpect(method(POST))
            .andExpect(header(AUTHORIZATION, "Bot $botToken"))
            .andExpect(
                content().json(
                    objectMapper.writeValueAsString(
                        mapOf("content" to message, "allowed_mentions" to mapOf("parse" to emptyList<String>()))
                    )
                )
            )
            .andRespond(withSuccess(objectMapper.writeValueAsBytes(mapOf("id" to "1")), APPLICATION_JSON))

        /** when */
        discordClient.sendMessage(directMessageChannelId, message)

        /** then */
        mockServer.verify()
    }

    @Test
    fun `회원이 서버 멤버의 DM 을 막아 두었으면 DM 차단 오류로 처리함`() {
        /** given */
        mockServer
            .expect(requestTo("https://discord.com/api/v10/channels/$directMessageChannelId/messages"))
            .andRespond(
                withStatus(FORBIDDEN)
                    .contentType(APPLICATION_JSON)
                    .body(
                        objectMapper.writeValueAsBytes(
                            mapOf("code" to 50007, "message" to "Cannot send messages to this user")
                        )
                    )
            )

        /** when */
        val exception = assertFailsWith<BusinessException> {
            discordClient.sendMessage(directMessageChannelId, "알림")
        }

        /** then */
        assertEquals(SocialAccountError.DISCORD_DIRECT_MESSAGE_BLOCKED, exception.error)
    }

    @Test
    fun `DM 차단이 아닌 이유로 메시지가 거부되면 외부 API 오류로 처리함`() {
        /** given */
        mockServer
            .expect(requestTo("https://discord.com/api/v10/channels/$directMessageChannelId/messages"))
            .andRespond(
                withStatus(FORBIDDEN)
                    .contentType(APPLICATION_JSON)
                    .body(objectMapper.writeValueAsBytes(mapOf("code" to 50001, "message" to "Missing Access")))
            )

        /** when */
        val exception = assertFailsWith<BusinessException> {
            discordClient.sendMessage(directMessageChannelId, "알림")
        }

        /** then */
        assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
    }
}
