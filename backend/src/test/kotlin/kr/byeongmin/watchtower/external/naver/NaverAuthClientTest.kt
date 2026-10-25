package kr.byeongmin.watchtower.external.naver

import kr.byeongmin.watchtower.external.naver.client.NaverAuthClient
import kr.byeongmin.watchtower.external.naver.dto.NaverProfileResponseDto
import kr.byeongmin.watchtower.external.naver.dto.NaverTokenResponseDto
import kr.byeongmin.watchtower.global.error.AuthError
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
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
import org.springframework.web.util.UriUtils
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NaverAuthClientTest {
    @Test
    fun `네이버 토큰 응답이 비어 있으면 외부 API 오류로 처리함`() {
        /** given */
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = createNaverAuthClient(builder.build())
        server.expect(anything()).andExpect(method(POST)).andRespond(withSuccess())

        /** when */
        val exception = assertFailsWith<BusinessException> {
            client.fetchToken("code", "state")
        }

        /** then */
        assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
        server.verify()
    }

    @Test
    fun `네이버 프로필 응답이 비어 있으면 외부 API 오류로 처리함`() {
        /** given */
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = createNaverAuthClient(builder.build())
        server.expect(anything()).andRespond(withSuccess())

        /** when */
        val exception = assertFailsWith<BusinessException> {
            client.fetchProfile("access")
        }

        /** then */
        assertEquals(CommonError.EXTERNAL_API_ERROR, exception.error)
        server.verify()
    }

    private val clientId = "네이버에서-부여해준-클라이언트-아이디"
    private val clientSecret = "네이버에서-부여해준-클라이언트-시크릿"
    private val callbackUrl = "https://watchtower.example/login/callback"
    private val objectMapper = jacksonObjectMapper()

    private val tokenResponse = NaverTokenResponseDto(
        accessToken = "네이버에서-발급한-엑세스-토큰",
        refreshToken = "네이버에서-발급한-리프레시-토큰",
        tokenType = "bearer",
        expiresIn = 3600L
    )
    private val profileResponse = NaverProfileResponseDto(
        resultCode = "00",
        message = "성공",
        response = NaverProfileResponseDto.NaverProfileDetailDto(
            providerId = "네이버-고객-고유-아이디",
            email = "네이버-고객-이메일",
            nickname = "네이버-고객-닉네임",
            profileImage = "https://naver.com/user/123/profile.png"
        )
    )

    private fun createNaverAuthClient(restClient: RestClient = mock()): NaverAuthClient {
        return NaverAuthClient(
            restClient = restClient,
            clientId = clientId,
            clientSecret = clientSecret,
            callbackUrl = callbackUrl
        )
    }

    @Test
    fun `네이버 인증 URL은 설정된 클라이언트와 콜백 주소 및 요청한 state를 사용함`() {
        /** given */
        val state = "프론트가-설정하는-CSRF-방지용-랜덤값"
        val naverAuthClient = createNaverAuthClient()

        /** when */
        val authorizationRequest = naverAuthClient.getAuthorizationRequest(state)

        /** then */
        val url = UriComponentsBuilder.fromUriString(authorizationRequest.authorizationRequestUri).build()
        assertEquals("https", url.scheme)
        assertEquals("nid.naver.com", url.host)
        assertEquals("/oauth2.0/authorize", url.path)
        assertEquals(
            mapOf(
                "response_type" to listOf("code"),
                "client_id" to listOf(clientId),
                "redirect_uri" to listOf(callbackUrl),
                "state" to listOf(state)
            ),
            url.queryParams.mapValues { (_, values) ->
                values.map { UriUtils.decode(it, StandardCharsets.UTF_8) }
            }
        )
    }

    @Test
    fun `state가 비어 있으면 네이버 인증 요청을 만들 수 없음`() {
        /** given */
        val naverAuthClient = createNaverAuthClient()

        /** when */
        val exception = assertFailsWith<BusinessException> {
            naverAuthClient.getAuthorizationRequest("")
        }

        /** then */
        assertEquals(AuthError.INVALID_OAUTH_STATE, exception.error)
    }

    @Test
    fun `네이버가 발급한 인증 코드와 state로 네이버 토큰을 받을 수 있음`() {
        /** given */
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

        mockServer
            .expect(requestTo(tokenUri))
            .andExpect(method(POST))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsBytes(
                        object {
                            val access_token = tokenResponse.accessToken
                            val refresh_token = tokenResponse.refreshToken
                            val token_type = tokenResponse.tokenType
                            val expires_in = tokenResponse.expiresIn
                        }
                    ),
                    APPLICATION_JSON
                )
            )

        /** when */
        val response = naverAuthClient.fetchToken(code, state)

        /** then */
        assertEquals(tokenResponse, response)
        mockServer.verify()
    }

    @Test
    fun `네이버 액세스 토큰으로 회원의 네이버 프로필을 가져올 수 있음`() {
        /** given */
        val restClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        val naverAuthClient = createNaverAuthClient(restClientBuilder.build())

        mockServer
            .expect(requestTo("https://openapi.naver.com/v1/nid/me"))
            .andExpect(method(GET))
            .andExpect(
                header(
                    AUTHORIZATION,
                    "Bearer ${tokenResponse.accessToken}"
                )
            )
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsBytes(
                        object {
                            val resultcode = profileResponse.resultCode
                            val message = profileResponse.message
                            val response = object {
                                val id = profileResponse.response.providerId
                                val email = profileResponse.response.email
                                val nickname = profileResponse.response.nickname
                                val profile_image = profileResponse.response.profileImage
                            }
                        }
                    ),
                    APPLICATION_JSON
                )
            )

        /** when */
        val response = naverAuthClient.fetchProfile(tokenResponse.accessToken)

        /** then */
        assertEquals(profileResponse, response)
        mockServer.verify()
    }
}
