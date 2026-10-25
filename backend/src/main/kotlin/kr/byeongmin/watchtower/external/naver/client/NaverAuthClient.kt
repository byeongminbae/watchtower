package kr.byeongmin.watchtower.external.naver.client

import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class NaverAuthClient(
    private val restClient: RestClient,
    @Value("\${watchtower.auth.naver.client-id}") private val clientId: String,
    @Value("\${watchtower.auth.naver.client-secret}") private val clientSecret: String,
    @Value("\${watchtower.auth.naver.callback-url}") private val callbackUrl: String
) {
    fun getAuthorizationRequest(state: String): OAuth2AuthorizationRequest {
        if (state.isBlank()) {
            throw BusinessException(AuthError.INVALID_OAUTH_STATE)
        }
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
            .clientId(clientId)
            .redirectUri(callbackUrl)
            .state(state)
            .build()
    }

    fun fetchToken(code: String, state: String): NaverTokenResponseDto {
        return restClient.post().uri { uriBuilder ->
            uriBuilder
                .scheme("https")
                .host("nid.naver.com")
                .path("/oauth2.0/token")
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("code", code)
                .queryParam("state", state)
                .build();
        }
            .retrieve()
            .body<NaverTokenResponseDto>()
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }

    fun fetchProfile(accessToken: String): NaverProfileResponseDto {
        return restClient.get()
            .uri("https://openapi.naver.com/v1/nid/me")
            .header(AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body<NaverProfileResponseDto>()
            ?: throw BusinessException(CommonError.EXTERNAL_API_ERROR)
    }
}
