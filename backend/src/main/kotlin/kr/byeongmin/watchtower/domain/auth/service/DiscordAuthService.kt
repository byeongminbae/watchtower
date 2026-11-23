package kr.byeongmin.watchtower.domain.auth.service

import kr.byeongmin.watchtower.domain.auth.accessor.DiscordAccountAccessor
import kr.byeongmin.watchtower.domain.auth.entity.DiscordAccount
import kr.byeongmin.watchtower.domain.auth.error.SocialAccountError
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.subscription.accessor.PlanAccessor
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.external.discord.client.DiscordClient
import kr.byeongmin.watchtower.external.discord.dto.DiscordUserResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.security.MemberPrinciple
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import kr.byeongmin.watchtower.global.utils.isNotNull
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DiscordAuthService(
    private val discordClient: DiscordClient,
    private val memberAccessor: MemberAccessor,
    private val discordAccountAccessor: DiscordAccountAccessor,
    private val planAccessor: PlanAccessor,
    private val timeProvider: TimeProvider
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val accessToken = userRequest.accessToken.tokenValue
        try {
            val discordUser = discordClient.fetchUser(accessToken)
            val signedInMember = SecurityContextHolder.getContext().authentication?.principal as? MemberPrinciple
            if (signedInMember.isNotNull()) {
                return linkDiscord(signedInMember, discordUser, accessToken)
            }
            return signInWithDiscord(discordUser, accessToken)
        } catch (exception: BusinessException) {
            throw OAuth2AuthenticationException(OAuth2Error(exception.error.statusCode), exception)
        }
    }

    private fun signInWithDiscord(discordUser: DiscordUserResponseDto, accessToken: String): MemberPrinciple {
        val discordAccount = discordAccountAccessor.findDiscordAccount(discordUser.id)
        val member = discordAccount?.member ?: discordSignUp(discordUser, accessToken)

        member.ensureSignInAllowed()

        member.lastSignInAt = timeProvider.now()
        return MemberPrinciple(member.id.ifNullThrow(), member.role.name)
    }

    private fun linkDiscord(
        memberPrinciple: MemberPrinciple,
        discordUser: DiscordUserResponseDto,
        accessToken: String
    ): MemberPrinciple {
        throwIfMemberHasDiscordAccount(memberPrinciple.memberId)
        throwIfLinkedToOtherMember(discordUser.id)

        val member = memberAccessor.getMember(memberPrinciple.memberId)
        saveDiscordAccount(member, discordUser.id, accessToken)
        return memberPrinciple
    }

    private fun throwIfMemberHasDiscordAccount(memberId: Long) {
        if (discordAccountAccessor.findDiscordAccountOfMember(memberId).isNotNull()) {
            throw BusinessException(SocialAccountError.PROVIDER_ALREADY_LINKED)
        }
    }

    private fun throwIfLinkedToOtherMember(discordUserId: String) {
        if (discordAccountAccessor.findDiscordAccount(discordUserId).isNotNull()) {
            throw BusinessException(SocialAccountError.LINKED_TO_OTHER_MEMBER)
        }
    }

    private fun discordSignUp(discordUser: DiscordUserResponseDto, accessToken: String): Member {
        val freePlan = planAccessor.findPlan(PlanType.FREE)
            ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR)

        val member = memberAccessor.save(
            Member.of(
                nickname = discordUser.getDisplayName(),
                profileImageUrl = discordUser.getAvatarUrl(),
                lastSignInAt = timeProvider.now(),
                plan = freePlan
            )
        )
        saveDiscordAccount(member, discordUser.id, accessToken)

        return member
    }

    private fun saveDiscordAccount(member: Member, discordUserId: String, accessToken: String) {
        discordClient.addGuildMember(discordUserId, accessToken)
        val directMessageChannelId = discordClient.createDirectMessageChannel(discordUserId)
        discordAccountAccessor.save(
            DiscordAccount.of(
                member = member,
                providerId = discordUserId,
                directMessageChannelId = directMessageChannelId
            )
        )
    }
}
