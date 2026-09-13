package kr.byeongmin.watchtower.domain.auth.service

import jakarta.transaction.Transactional
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.auth.repository.NaverOAuthRepository
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.domain.member.service.MemberTokenIssuer
import kr.byeongmin.watchtower.external.naver.client.NaverAuthClient
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
    fun getNaverSignInUrl(state: String): SuccessDataResponse<String> {
        return SuccessDataResponse(naverAuthClient.buildSignInUrl(state))
    }

    @Transactional
    fun signInWithNaverCallback(code: String, state: String): SuccessDataResponse<MemberTokenResponseDto> {
        val naverTokenResponse = naverAuthClient.fetchToken(code, state)
        val naverProfileResponse = naverAuthClient.fetchProfile(naverTokenResponse.accessToken)
        val providerId = naverProfileResponse.response.providerId

        return if (naverOAuthRepository.existsByProviderId(providerId)) {
            naverSignIn(naverProfileResponse)
        } else {
            naverSignUpThenSignIn(
                naverProfileResponseDto = naverProfileResponse,
                naverTokenResponseDto = naverTokenResponse
            )
        }
    }

    private fun naverSignIn(
        naverProfileResponseDto: NaverProfileResponseDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val naverOAuth = naverOAuthRepository.findByProviderId(naverProfileResponseDto.response.providerId)
        val memberTokenResponseDto = memberTokenIssuer.signIn(naverOAuth.member)

        return SuccessDataResponse(memberTokenResponseDto)
    }

    private fun naverSignUpThenSignIn(
        naverProfileResponseDto: NaverProfileResponseDto,
        naverTokenResponseDto: NaverTokenResponseDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val member = memberRepository.save(Member.from(naverProfileResponseDto))
        naverOAuthRepository.save(
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
        val member = memberRepository.findById(memberId).getOrNull()
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)

        return SuccessDataResponse(memberTokenIssuer.rotate(member))
    }

    @Transactional
    fun logout(memberId: Long): SuccessResponse {
        val member = memberRepository.findById(memberId).getOrNull()
            ?: throw BusinessException(CommonError.RESOURCE_NOT_FOUND)

        member.signOut()

        return SuccessResponse()
    }
}
