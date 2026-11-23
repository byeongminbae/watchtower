package kr.byeongmin.watchtower.domain.auth.service

import kr.byeongmin.watchtower.domain.auth.accessor.DiscordAccountAccessor
import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.enums.MemberRole
import kr.byeongmin.watchtower.domain.subscription.accessor.PlanAccessor
import kr.byeongmin.watchtower.domain.subscription.entity.Plan
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.external.discord.dto.DiscordUserResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class DiscordAuthServiceTest {
    private val memberAccessor = mock<MemberAccessor>()
    private val discordAccountAccessor = mock<DiscordAccountAccessor>()
    private val planAccessor = mock<PlanAccessor>()
    private val now = LocalDateTime.of(2026, 10, 20, 12, 0)
    private val timeProvider = mock<TimeProvider> {
        on { now() } doReturn now
    }
    private val discordClient = mock<DiscordClient>()
    private val freePlan = mock<Plan> {
        on { type } doReturn PlanType.FREE
    }
    private val discordAuthService = DiscordAuthService(
        discordClient = discordClient,
        memberAccessor = memberAccessor,
        discordAccountAccessor = discordAccountAccessor,
        planAccessor = planAccessor,
        timeProvider = timeProvider
    )

    private val accessToken = "Discord-가-발급한-회원-액세스-토큰"
    private val userRequest = OAuth2UserRequest(
        ClientRegistration.withRegistrationId("discord")
            .clientId("discord-application-id")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://watchtower.test/bff/oauth2/callback/discord")
            .authorizationUri("https://discord.com/oauth2/authorize")
            .tokenUri("https://discord.com/api/v10/oauth2/token")
            .build(),
        OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, accessToken, Instant.now(), Instant.now().plusSeconds(60))
    )
    private val directMessageChannelId = "1573265772178968700"
    private val discordUser = DiscordUserResponseDto(
        id = "80351110224678912",
        username = "nelly",
        globalName = "넬리",
        avatar = null
    )
    private val member = mock<Member> {
        on { id } doReturn 31L
        on { role } doReturn MemberRole.NORMAL
    }
    private val discordAccount = DiscordAccount.of(
        member = member,
        providerId = discordUser.id,
        directMessageChannelId = directMessageChannelId
    )

    @BeforeEach
    fun setUpDiscordUser() {
        whenever(discordClient.fetchUser(accessToken)).thenReturn(discordUser)
    }

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Nested
    inner class SignInWithDiscordTest {
        @Test
        fun `이미 가입한 Discord 회원은 로그인 시각을 저장하고 회원 정보를 반환함`() {
            /** given */
            whenever(discordAccountAccessor.findDiscordAccount(discordUser.id)).thenReturn(discordAccount)

            /** when */
            val result = assertIs<MemberPrinciple>(discordAuthService.loadUser(userRequest))

            /** then */
            assertEquals(31L, result.memberId)
            assertEquals(MemberRole.NORMAL.name, result.role)
            inOrder(member) {
                verify(member).ensureSignInAllowed()
                verify(member).lastSignInAt = now
            }
            verify(memberAccessor, never()).save(any<Member>())
            verify(discordClient, never()).addGuildMember(any(), any())
        }

        @Test
        fun `처음 로그인하는 Discord 회원은 무료 플랜으로 가입하고 서버에 참여시킨 뒤 DM 채널을 연결함`() {
            /** given */
            whenever(discordAccountAccessor.findDiscordAccount(discordUser.id)).thenReturn(null)
            whenever(planAccessor.findPlan(PlanType.FREE)).thenReturn(freePlan)
            whenever(memberAccessor.save(any<Member>())).thenReturn(member)
            whenever(discordClient.createDirectMessageChannel(discordUser.id)).thenReturn(directMessageChannelId)

            /** when */
            val result = assertIs<MemberPrinciple>(discordAuthService.loadUser(userRequest))

            /** then */
            assertEquals(31L, result.memberId)
            assertEquals(MemberRole.NORMAL.name, result.role)
            val savedMember = argumentCaptor<Member>()
            verify(memberAccessor).save(savedMember.capture())
            assertEquals(discordUser.getDisplayName(), savedMember.firstValue.nickname)
            assertEquals(discordUser.getAvatarUrl(), savedMember.firstValue.profileImageUrl)
            assertEquals(now, savedMember.firstValue.lastSignInAt)
            assertEquals(PlanType.FREE, savedMember.firstValue.plan.type)
            val savedDiscordAccount = argumentCaptor<DiscordAccount>()
            inOrder(discordClient, discordAccountAccessor) {
                verify(discordClient).addGuildMember(discordUser.id, accessToken)
                verify(discordClient).createDirectMessageChannel(discordUser.id)
                verify(discordAccountAccessor).save(savedDiscordAccount.capture())
            }
            assertEquals(member, savedDiscordAccount.firstValue.member)
            assertEquals(discordUser.id, savedDiscordAccount.firstValue.providerId)
            assertEquals(directMessageChannelId, savedDiscordAccount.firstValue.directMessageChannelId)
            verify(member).ensureSignInAllowed()
            verify(member).lastSignInAt = now
        }

        @Test
        fun `로그인할 수 없는 회원은 로그인 시각을 갱신하지 않고 오류 코드를 담은 OAuth 인증 오류로 거부함`() {
            /** given */
            whenever(discordAccountAccessor.findDiscordAccount(discordUser.id)).thenReturn(discordAccount)
            doThrow(BusinessException(AuthError.BANNED_ACCOUNT)).whenever(member).ensureSignInAllowed()

            /** when */
            val exception = assertFailsWith<OAuth2AuthenticationException> {
                discordAuthService.loadUser(userRequest)
            }

            /** then */
            assertEquals(AuthError.BANNED_ACCOUNT.statusCode, exception.error.errorCode)
            verify(member, never()).lastSignInAt = any()
        }

        @Test
        fun `무료 플랜이 준비되지 않으면 신규 회원이 가입할 수 없음`() {
            /** given */
            whenever(discordAccountAccessor.findDiscordAccount(discordUser.id)).thenReturn(null)
            whenever(planAccessor.findPlan(PlanType.FREE)).thenReturn(null)

            /** when */
            val exception = assertFailsWith<OAuth2AuthenticationException> {
                discordAuthService.loadUser(userRequest)
            }

            /** then */
            assertEquals(CommonError.INTERNAL_SERVER_ERROR.statusCode, exception.error.errorCode)
            verifyNoInteractions(memberAccessor)
            verify(discordClient, never()).addGuildMember(any(), any())
            verify(discordAccountAccessor, never()).save(any())
        }
    }

    @Nested
    inner class LinkDiscordTest {
        private val memberPrinciple = MemberPrinciple(31L, MemberRole.NORMAL.name)

        @BeforeEach
        fun signIn() {
            SecurityContextHolder.getContext().authentication = memberPrinciple.to()
        }

        @Test
        fun `로그인한 회원이 연결되지 않은 Discord 계정으로 인증하면 서버에 참여시키고 계정을 연결함`() {
            /** given */
            whenever(discordAccountAccessor.findDiscordAccount(discordUser.id)).thenReturn(null)
            whenever(discordAccountAccessor.findDiscordAccountOfMember(memberPrinciple.memberId)).thenReturn(null)
            whenever(memberAccessor.getMember(memberPrinciple.memberId)).thenReturn(member)
            whenever(discordClient.createDirectMessageChannel(discordUser.id)).thenReturn(directMessageChannelId)

            /** when */
            val result = discordAuthService.loadUser(userRequest)

            /** then */
            assertSame(memberPrinciple, result)
            val savedDiscordAccount = argumentCaptor<DiscordAccount>()
            inOrder(discordClient, discordAccountAccessor) {
                verify(discordClient).addGuildMember(discordUser.id, accessToken)
                verify(discordClient).createDirectMessageChannel(discordUser.id)
                verify(discordAccountAccessor).save(savedDiscordAccount.capture())
            }
            assertEquals(member, savedDiscordAccount.firstValue.member)
            assertEquals(directMessageChannelId, savedDiscordAccount.firstValue.directMessageChannelId)
            verify(memberAccessor, never()).save(any<Member>())
        }

        @Test
        fun `다른 회원에 연결된 Discord 계정은 연결할 수 없음`() {
            /** given */
            val otherMember = mock<Member> {
                on { id } doReturn 99L
            }
            val otherMemberDiscordAccount = DiscordAccount.of(otherMember, discordUser.id, directMessageChannelId)
            whenever(discordAccountAccessor.findDiscordAccount(discordUser.id)).thenReturn(otherMemberDiscordAccount)

            /** when */
            val exception = assertFailsWith<OAuth2AuthenticationException> {
                discordAuthService.loadUser(userRequest)
            }

            /** then */
            assertEquals(SocialAccountError.LINKED_TO_OTHER_MEMBER.statusCode, exception.error.errorCode)
            verify(discordAccountAccessor, never()).save(any())
        }

        @Test
        fun `Discord 계정이 이미 연결된 회원은 같은 계정이어도 Discord 계정을 다시 연결할 수 없음`() {
            /** given */
            whenever(discordAccountAccessor.findDiscordAccountOfMember(memberPrinciple.memberId))
                .thenReturn(discordAccount)

            /** when */
            val exception = assertFailsWith<OAuth2AuthenticationException> {
                discordAuthService.loadUser(userRequest)
            }

            /** then */
            assertEquals(SocialAccountError.PROVIDER_ALREADY_LINKED.statusCode, exception.error.errorCode)
            verify(discordClient, never()).addGuildMember(any(), any())
            verify(discordAccountAccessor, never()).save(any())
        }
    }
}
