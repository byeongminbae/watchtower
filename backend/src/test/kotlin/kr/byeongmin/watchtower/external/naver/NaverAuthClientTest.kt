package kr.byeongmin.watchtower.external.naver

import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

class NaverAuthClientTest {
    private val clientId = "네이버에서-부여해준-클라이언트-아이디"
    private val clientSecret = "네이버에서-부여해준-클라이언트-시크릿"
    private val callbackUrl = "네이버에서-우리쪽으로-code와-state를-넘겨주는-콜백주소"
    private val objectMapper = jacksonObjectMapper()

    private val tokenResponse = NaverTokenResponseDto(
        accessToken = "네이버에서-발급한-엑세스-토큰",
        refreshToken = "네이버에서-발급한-리프레시-토큰",
        tokenType = "bearer",
        expiresIn = 3600L,
    )
    private val profileResponse = NaverProfileResponseDto(
        resultCode = "00",
        message = "성공",
        response = NaverProfileResponseDto.NaverProfileDetailDto(
            providerId = "네이버-고객-고유-아이디",
            email = "네이버-고객-이메일",
            nickname = "네이버-고객-닉네임",
            profileImage = "https://naver.com/user/123/profile.png",
        ),
    )

    private fun createNaverAuthClient(restClient: RestClient = mock()): NaverAuthClient {
        return NaverAuthClient(
            restClient = restClient,
            naverClientId = clientId,
            naverClientSecret = clientSecret,
            callbackUrl = callbackUrl,
        )
    }

    @Test
    fun `로그인 상태값이 주어진 상황에서 네이버 로그인 URL을 생성하면 OAuth 설정값을 포함한다`() {
        // Given
        val state = "프론트가-설정하는-CSRF-방지용-랜덤값"
        val naverAuthClient = createNaverAuthClient()

        // When
        val signInUrl = naverAuthClient.buildSignInUrl(state)

        // Then
        val url = UriComponentsBuilder.fromUriString(signInUrl).build()
        assertEquals("https", url.scheme)
        assertEquals("nid.naver.com", url.host)
        assertEquals("/oauth2.0/authorize", url.path)
        assertEquals(
            mapOf(
                "response_type" to listOf("code"),
                "client_id" to listOf(clientId),
                "redirect_uri" to listOf(callbackUrl),
                "state" to listOf(state),
            ),
            url.queryParams,
        )
    }

    @Test
    fun `인가 코드와 상태값이 주어진 상황에서 토큰을 요청하면 네이버가 응답한 토큰을 반환한다`() {
        // Given
        val code = "네이버가-콜백으로-넘겨준-인가-코드"
        val state = "네이버가-콜백으로-넘겨주는-CSRF-방지용-랜덤값"
        val restClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        val naverAuthClient = createNaverAuthClient(restClientBuilder.build())

        val tokenUri = UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/token")
            .queryParam("grant_type", "authorization_code")
            .queryParam("client_id", clientId)
            .queryParam("client_secret", clientSecret)
            .queryParam("redirect_uri", callbackUrl)
            .queryParam("code", code)
            .queryParam("state", state)
            .build()
            .encode()
            .toUri()

        mockServer.expect(requestTo(tokenUri))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(tokenResponse),
                    APPLICATION_JSON
                )
            )

        // When
        val response = naverAuthClient.fetchToken(code, state)

        // Then
        assertEquals(tokenResponse, response)
        mockServer.verify()
    }

    @Test
    fun `엑세스 토큰이 주어진 상황에서 프로필을 요청하면 네이버가 응답한 프로필을 반환한다`() {
        // Given
        val restClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        val naverAuthClient = createNaverAuthClient(restClientBuilder.build())

        mockServer.expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andExpect(method(GET))
            .andExpect(
                header(
                    AUTHORIZATION,
                    "Bearer ${tokenResponse.accessToken}"
                )
            )
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(profileResponse),
                    APPLICATION_JSON
                )
            )

        // When
        val response = naverAuthClient.fetchProfile(tokenResponse.accessToken)

        // Then
        assertEquals(profileResponse, response)
        mockServer.verify()
    }
}
