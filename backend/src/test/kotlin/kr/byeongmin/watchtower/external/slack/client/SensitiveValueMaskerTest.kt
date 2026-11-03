package kr.byeongmin.watchtower.external.slack.client

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SensitiveValueMaskerTest {
    @Test
    fun `요청 파라미터의 인가 코드와 state 는 가리고 나머지 파라미터는 그대로 둠`() {
        /** given */
        val text = "parameterMap: code=abc123, state=xyz789, page=2"

        /** when */
        val result = SensitiveValueMasker.mask(text)

        /** then */
        assertEquals("parameterMap: code=***, state=***, page=2", result)
    }

    @Test
    fun `이름에 code 가 들어간 다른 키는 가리지 않음`() {
        /** given */
        val text = "statusCode=EXT_000, errorCode=TIMEOUT"

        /** when */
        val result = SensitiveValueMasker.mask(text)

        /** then */
        assertEquals(text, result)
    }

    @Test
    fun `JSON 본문의 토큰 값을 가림`() {
        /** given */
        val text = """{"access_token":"aaa","refresh_token":"bbb","token_type":"bearer"}"""

        /** when */
        val result = SensitiveValueMasker.mask(text)

        /** then */
        assertEquals("""{"access_token":"***","refresh_token":"***","token_type":"bearer"}""", result)
    }

    @Test
    fun `Bearer 토큰을 가림`() {
        /** given */
        val text = "Authorization: Bearer eyJhbGciOi.payload.signature"

        /** when */
        val result = SensitiveValueMasker.mask(text)

        /** then */
        assertEquals("Authorization: Bearer ***", result)
    }

    @Test
    fun `Slack 과 Discord 웹훅 URL 의 경로를 가림`() {
        /** given */
        val text = "https://hooks.slack.com/services/T0/B0/secret https://discord.com/api/webhooks/1/secret"

        /** when */
        val result = SensitiveValueMasker.mask(text)

        /** then */
        assertEquals("https://hooks.slack.com/*** https://discord.com/api/webhooks/***", result)
    }
}
