package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.security.JwtProvider
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.stereotype.Component

@Component
class MemberTokenIssuer(
    private val jwtProvider: JwtProvider
) {
    private fun getAccessToken(member: Member): String {
        return jwtProvider.createAccessToken(
            memberId = member.id.ifNullThrow(),
            role = member.role.name
        )
    }

    private fun getRefreshToken(member: Member): String {
        return jwtProvider.createRefreshToken(
            memberId = member.id.ifNullThrow()
        )
    }

    fun signIn(member: Member): MemberTokenResponseDto {
        val accessToken = getAccessToken(member)
        val refreshToken = getRefreshToken(member)

        member.completeSignIn(refreshToken)

        return MemberTokenResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiry = jwtProvider.accessTokenExpiry
        )
    }

    fun rotate(member: Member): MemberTokenResponseDto {
        val accessToken = getAccessToken(member)
        val refreshToken = getRefreshToken(member)

        member.rotateRefreshToken(refreshToken)

        return MemberTokenResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiry = jwtProvider.accessTokenExpiry
        )
    }
}