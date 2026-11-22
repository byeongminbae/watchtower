package kr.byeongmin.watchtower.external.discord.dto

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DiscordUserResponseDtoTest {
    @Test
    fun `프로필 이미지를 올린 사용자는 해당 이미지 주소를 사용함`() {
        /** given */
        val discordUser = DiscordUserResponseDto(
            id = "80351110224678912",
            username = "nelly",
            globalName = null,
            avatar = "8342729096ea3675442027381ff50dfe"
        )

        /** when */
        val result = discordUser.getAvatarUrl()

        /** then */
        assertEquals(
            "https://cdn.discordapp.com/avatars/80351110224678912/8342729096ea3675442027381ff50dfe.png",
            result
        )
    }

    @Test
    fun `프로필 이미지가 없는 사용자는 사용자 ID 로 정해지는 기본 이미지 주소를 사용함`() {
        /** given */
        val discordUser = DiscordUserResponseDto(
            id = "80351110224678912",
            username = "nelly",
            globalName = null,
            avatar = null
        )

        /** when */
        val result = discordUser.getAvatarUrl()

        /** then */
        assertEquals("https://cdn.discordapp.com/embed/avatars/5.png", result)
    }

    @Test
    fun `부호 있는 정수 범위를 넘는 사용자 ID 도 부호 없는 64비트로 기본 이미지 주소를 계산함`() {
        /** given */
        val discordUser = DiscordUserResponseDto(
            id = "9223372036854775808",
            username = "nelly",
            globalName = null,
            avatar = null
        )

        /** when */
        val result = discordUser.getAvatarUrl()

        /** then */
        assertEquals("https://cdn.discordapp.com/embed/avatars/2.png", result)
    }

    @Test
    fun `표시 이름을 정하지 않은 사용자는 사용자명을 이름으로 사용함`() {
        /** given */
        val discordUser = DiscordUserResponseDto(
            id = "80351110224678912",
            username = "nelly",
            globalName = null,
            avatar = null
        )

        /** when */
        val result = discordUser.getDisplayName()

        /** then */
        assertEquals("nelly", result)
    }
}
