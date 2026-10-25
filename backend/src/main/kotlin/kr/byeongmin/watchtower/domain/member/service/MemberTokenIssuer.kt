package kr.byeongmin.watchtower.domain.member.service

import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.global.security.JwtProvider
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.utils.ifNullThrow
import org.springframework.stereotype.Component

/** 멤버를 위한 토큰 작업 후 멤버 엔티티에 대한 업데이트를 누락할까봐 만든 [JwtProvider] 래핑 클래스
 * JwtProvider 는 전역 패키지라 domain 패키지에 의존해선 안되기 때문에 래핑을 만듦
 * TODO: JWT 관련 클래스가 JwtProvider 와 MemberTokenIssuer 이렇게 두개가 있으니까 처음 보는 입장에선 헷갈릴 수도 있음.. 클래스명 개선이 필요하지 않을까? 생각해봄
 */
@Component
class MemberTokenIssuer(
    private val jwtProvider: JwtProvider,
    private val timeProvider: TimeProvider
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

    /** 액세스랑 리프레시 토큰을 발급하고 회원에 로그인 완료 상태를 반영 */
    fun signIn(member: Member): MemberTokenResponseDto {
        val accessToken = getAccessToken(member)
        val refreshToken = getRefreshToken(member)

        member.completeSignIn(refreshToken, timeProvider.now())

        return MemberTokenResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = jwtProvider.accessTokenExpiry,
            refreshTokenExpiresIn = jwtProvider.refreshTokenExpiry
        )
    }

    /** 액세스랑 리프레시 토큰을 재발급하고 최근 이용 시각을 갱신 */
    fun rotate(member: Member): MemberTokenResponseDto {
        val accessToken = getAccessToken(member)
        val refreshToken = getRefreshToken(member)

        member.rotateRefreshToken(refreshToken, timeProvider.now())

        return MemberTokenResponseDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = jwtProvider.accessTokenExpiry,
            refreshTokenExpiresIn = jwtProvider.refreshTokenExpiry
        )
    }
}
