package kr.byeongmin.watchtower.domain.auth.service

import jakarta.transaction.Transactional
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.auth.repository.NaverOAuthRepository
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.domain.member.service.MemberTokenIssuer
import kr.byeongmin.watchtower.external.naver.NaverAuthClient
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.response.SuccessDataResponse
import kr.byeongmin.watchtower.global.response.SuccessResponse
import kr.byeongmin.watchtower.global.security.JwtProvider
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class AuthService(
    private val naverAuthClient: NaverAuthClient,
    private val memberRepository: MemberRepository,
    private val naverOAuthRepository: NaverOAuthRepository,
    private val memberTokenIssuer: MemberTokenIssuer,
    private val jwtProvider: JwtProvider,
) {
    fun getNaverSignInUrl(
        state: String
    ): SuccessDataResponse<String> {
        return SuccessDataResponse(naverAuthClient.buildSignInUrl(state))
    }

    @Transactional
    fun signInWithNaverCallback(
        code: String,
        state: String
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val naverMemberToken = naverAuthClient.fetchToken(code, state)
        val naverMemberProfile = naverAuthClient.fetchProfile(naverMemberToken.accessToken)

        if (naverOAuthRepository.existsByProviderId(naverMemberProfile.response.providerId)) {
            return naverMemberSignIn(naverMemberProfile)
        }
        return naverMemberSignUpThenSignIn(naverMemberProfile, naverMemberToken)
    }

    private fun naverMemberSignIn(
        naverMemberProfile: NaverProfileResponseDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val naverOAuth = naverOAuthRepository.findByProviderId(naverMemberProfile.response.providerId)
        return SuccessDataResponse(
            memberTokenIssuer.signIn(naverOAuth.member)
        )
    }

    private fun naverMemberSignUpThenSignIn(
        naverMemberProfile: NaverProfileResponseDto,
        naverMemberToken: NaverTokenResponseDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val member = memberRepository.save(
            Member.from(naverMemberProfile)
        )
        naverOAuthRepository.save(
            NaverOAuth.from(
                member = member,
                naverMemberProfile = naverMemberProfile,
                naverMemberToken = naverMemberToken
            )
        )

        return SuccessDataResponse(
            data = memberTokenIssuer.signIn(member)
        )
    }

    @Transactional
    fun renewToken(refreshToken: String): SuccessDataResponse<MemberTokenResponseDto> {
        val memberId = jwtProvider.getMemberId(refreshToken)
        val member = memberRepository.findById(memberId).getOrNull()
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)

        return SuccessDataResponse(
            data = memberTokenIssuer.rotate(member)
        )
    }

    @Transactional
    fun logout(memberId: Long): SuccessResponse {
        val member = memberRepository.findById(memberId).getOrNull()
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)

        member.signOut()

        return SuccessResponse()
    }
}
