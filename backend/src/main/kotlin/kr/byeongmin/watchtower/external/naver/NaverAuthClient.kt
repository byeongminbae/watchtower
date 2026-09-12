package kr.byeongmin.watchtower.external.naver

import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder

@Component
class NaverAuthClient(
    private val restClient: RestClient,
    @Value("\${watchtower.auth.naver.client-id}") private val naverClientId: String,
    @Value("\${watchtower.auth.naver.client-secret}") private val naverClientSecret: String,
    @Value("\${watchtower.auth.naver.callback-url}") private val callbackUrl: String
) {
    fun buildSignInUrl(
        state: String
    ): String {
        return UriComponentsBuilder.newInstance()
            .scheme("https")
            .host("nid.naver.com")
            .path("/oauth2.0/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", naverClientId)
            .queryParam("redirect_uri", callbackUrl)
            .queryParam("state", state)
            .build().toUriString()
    }

    fun fetchToken(
        code: String,
        state: String
    ): NaverTokenResponseDto {
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
            .body<NaverTokenResponseDto>()
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }

    fun fetchProfile(
        accessToken: String
    ): NaverProfileResponseDto {
        return restClient.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header(AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body<NaverProfileResponseDto>()
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }
}
