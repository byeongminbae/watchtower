package kr.byeongmin.watchtower.domain.auth.service

import jakarta.transaction.Transactional
import kr.byeongmin.watchtower.domain.auth.dto.MemberTokenResponseDto
import kr.byeongmin.watchtower.domain.auth.dto.NaverProfileResponseExternalDto
import kr.byeongmin.watchtower.domain.auth.dto.NaverTokenResponseExternalDto
import kr.byeongmin.watchtower.domain.auth.entity.NaverOAuth
import kr.byeongmin.watchtower.domain.auth.repository.NaverOAuthRepository
import kr.byeongmin.watchtower.domain.member.entity.Member
import kr.byeongmin.watchtower.domain.member.repository.MemberRepository
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import kr.byeongmin.watchtower.global.response.SuccessDataResponse
import kr.byeongmin.watchtower.global.security.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Service
class AuthService(
    private val restClient: RestClient,
    private val memberRepository: MemberRepository,
    private val naverOAuthRepository: NaverOAuthRepository,
    private val jwtProvider: JwtProvider,
    @Value("\${watchtower.auth.naver.client-id}") private val naverClientId: String,
    @Value("\${watchtower.auth.naver.client-secret}") private val naverClientSecret: String,
    @Value("\${watchtower.auth.naver.callback-url}") private val callbackUrl: String
) {
    fun getNaverLoginUrl(
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

    fun loginWithNaverCallback(
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

    @Transactional
    private fun naverMemberSignUpThenSignIn(
        naverMemberProfile: NaverProfileResponseExternalDto,
        naverMemberToken: NaverTokenResponseExternalDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val member = memberRepository.save(
            Member(
                email = naverMemberProfile.response.email,
                nickname = naverMemberProfile.response.nickname,
                profileImageUrl = naverMemberProfile.response.profileImage
            )
        )
        naverOAuthRepository.save(
            NaverOAuth(
                member = member,
                providerId = naverMemberProfile.response.id,
                accessToken = naverMemberToken.accessToken,
                refreshToken = naverMemberToken.refreshToken,
                expiredAt = naverMemberToken.expiresIn
            )
        )
        return SuccessDataResponse(
            MemberTokenResponseDto(
                accessToken = jwtProvider.createAccessToken(
                    member.id ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR),
                    member.role.name
                ),
                refreshToken = jwtProvider.createRefreshToken(
                    member.id ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR)
                ),
                accessTokenExpiry = jwtProvider.accessTokenExpiry
            )
        )
    }

    private fun naverMemberSignIn(
        naverMemberProfile: NaverProfileResponseExternalDto
    ): SuccessDataResponse<MemberTokenResponseDto> {
        val naverOAuth = naverOAuthRepository.findByProviderId(naverMemberProfile.response.id)
        return SuccessDataResponse(
            MemberTokenResponseDto(
                accessToken = jwtProvider.createAccessToken(
                    naverOAuth.member.id ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR),
                    naverOAuth.member.role.name
                ),
                refreshToken = jwtProvider.createRefreshToken(
                    naverOAuth.member.id ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR)
                ),
                accessTokenExpiry = jwtProvider.accessTokenExpiry
            )
        )
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
}
