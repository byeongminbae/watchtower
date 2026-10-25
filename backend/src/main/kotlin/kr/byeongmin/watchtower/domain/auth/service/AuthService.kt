package kr.byeongmin.watchtower.domain.auth.service

import kr.byeongmin.watchtower.domain.auth.accessor.NaverOAuthAccessor
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.member.accessor.MemberAccessor
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.service.MemberTokenIssuer
import kr.byeongmin.watchtower.domain.subscription.accessor.PlanAccessor
import kr.byeongmin.watchtower.domain.subscription.enums.PlanType
import kr.byeongmin.watchtower.external.naver.client.NaverAuthClient
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.config.TimeProvider
import kr.byeongmin.watchtower.global.dto.SuccessDataResponse
import kr.byeongmin.watchtower.global.dto.SuccessResponse
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.security.JwtProvider
import kr.byeongmin.watchtower.global.utils.isNotNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val naverAuthClient: NaverAuthClient,
    private val memberAccessor: MemberAccessor,
    private val naverOAuthAccessor: NaverOAuthAccessor,
    private val planAccessor: PlanAccessor,
    private val memberTokenIssuer: MemberTokenIssuer,
    private val jwtProvider: JwtProvider,
    private val timeProvider: TimeProvider
) {
    fun getNaverSignInUrl(state: String): SuccessDataResponse<String> {
        val naverSignInUrl = naverAuthClient.buildSignInUrl(state)
        return SuccessDataResponse(naverSignInUrl)
    }

    @Transactional
    fun signInWithNaverCallback(code: String, state: String): SuccessDataResponse<MemberTokenResponseDto> {
        val naverTokenResponseDto = naverAuthClient.fetchToken(code, state)
        val naverProfileResponseDto = naverAuthClient.fetchProfile(naverTokenResponseDto.accessToken)
        val providerId = naverProfileResponseDto.response.providerId

        val naverOAuth = naverOAuthAccessor.findByProviderId(providerId)

        return if (naverOAuth.isNotNull()) {
            val memberTokenResponseDto = memberTokenIssuer.signIn(naverOAuth.member)
            SuccessDataResponse(memberTokenResponseDto)
        } else {
            naverSignUpThenSignIn(
                naverProfileResponseDto = naverProfileResponseDto,
                naverTokenResponseDto = naverTokenResponseDto
            )
        }
    }

    private fun naverSignUpThenSignIn(
        naverProfileResponseDto: NaverProfileResponseDto,
        naverTokenResponseDto: NaverTokenResponseDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val freePlan = planAccessor.findByType(PlanType.FREE)
            ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR)

        val member = memberAccessor.save(
            Member.from(
                naverProfileResponseDto = naverProfileResponseDto,
                now = timeProvider.now(),
                plan = freePlan
            )
        )
        naverOAuthAccessor.save(
            NaverOAuth.from(
                member = member,
                naverProfileResponseDto = naverProfileResponseDto,
                naverTokenResponseDto = naverTokenResponseDto
            )
        )

        val memberTokenResponseDto = memberTokenIssuer.signIn(member)
        return SuccessDataResponse(memberTokenResponseDto)
    }

    @Transactional
    fun renewToken(refreshToken: String): SuccessDataResponse<MemberTokenResponseDto> {
        val memberId = jwtProvider.getMemberId(refreshToken)
        val member = memberAccessor.getMember(memberId)
        // TODO: 어차피 세션으로 갈거고 토큰 인증은 없앨 코드라서 동시 로그인 기능이랑 refresh token 재사용 검증코드 안넣음

        val memberTokenResponseDto = memberTokenIssuer.rotate(member)
        return SuccessDataResponse(memberTokenResponseDto)
    }

    @Transactional
    fun logout(memberId: Long): SuccessResponse {
        val member = memberAccessor.getMember(memberId)

        member.signOut()

        return SuccessResponse()
    }
}
