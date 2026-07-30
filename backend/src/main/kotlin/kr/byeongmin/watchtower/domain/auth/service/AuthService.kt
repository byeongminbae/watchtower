package kr.byeongmin.watchtower.domain.auth.service

import jakarta.transaction.Transactional
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.dto.NaverProfileResponseExternalDto
import kr.byeongmin.watchtower.domain.auth.dto.NaverTokenResponseExternalDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.auth.repository.NaverOAuthRepository
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.domain.member.service.MemberTokenIssuer
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.response.SuccessDataResponse
import kr.byeongmin.watchtower.global.response.SuccessResponse
import kr.byeongmin.watchtower.global.security.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import kotlin.jvm.optionals.getOrNull

@Service
class AuthService(
    private val restClient: RestClient,
    private val memberRepository: MemberRepository,
    private val naverOAuthRepository: NaverOAuthRepository,
    private val memberTokenIssuer: MemberTokenIssuer,
    private val jwtProvider: JwtProvider,
    @Value("\${watchtower.auth.naver.client-id}") private val naverClientId: String,
    @Value("\${watchtower.auth.naver.client-secret}") private val naverClientSecret: String,
    @Value("\${watchtower.auth.naver.callback-url}") private val callbackUrl: String
) {
    fun getNaverSignInUrl(
        state: String
    ): SuccessDataResponse<String> {
        val url = UriComponentsBuilder.newInstance()
            .path("https://nid.naver.com/oauth2.0/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", naverClientId)
            .queryParam("redirect_uri", callbackUrl)
            .queryParam("state", state)
            .build().toUriString()

        return SuccessDataResponse(url)
    }

    @Transactional
    fun signInWithNaverCallback(
        code: String,
        state: String
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val naverMemberToken = getNaverMemberToken(code, state)
        val naverMemberProfile = getNaverMemberProfile(naverMemberToken)

        if (naverOAuthRepository.existsByProviderId(naverMemberProfile.response.id)) {
            return naverMemberSignIn(naverMemberProfile)
        }
        return naverMemberSignUpThenSignIn(naverMemberProfile, naverMemberToken)
    }

    private fun getNaverMemberToken(
        code: String,
        state: String
    ): NaverTokenResponseExternalDto {
        return restClient.post().uri { uriBuilder ->
            uriBuilder
                .scheme("https")
                .host("nid.naver.com")
                .path("/oauth2.0/token")
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", naverClientId)
                .queryParam("client_secret", naverClientSecret)
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("code", code)
                .queryParam("state", state)
                .build();
        }
            .retrieve()
            .body(NaverTokenResponseExternalDto::class.java)
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }

    private fun getNaverMemberProfile(
        naverToken: NaverTokenResponseExternalDto
    ): NaverProfileResponseExternalDto {
        return restClient.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header(AUTHORIZATION, "Bearer ${naverToken.accessToken}")
            .retrieve()
            .body(NaverProfileResponseExternalDto::class.java)
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }

    private fun naverMemberSignIn(
        naverMemberProfile: NaverProfileResponseExternalDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val naverOAuth = naverOAuthRepository.findByProviderId(naverMemberProfile.response.id)
        return SuccessDataResponse(
            memberTokenIssuer.signIn(naverOAuth.member)
        )
    }

    private fun naverMemberSignUpThenSignIn(
        naverMemberProfile: NaverProfileResponseExternalDto,
        naverMemberToken: NaverTokenResponseExternalDto
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
