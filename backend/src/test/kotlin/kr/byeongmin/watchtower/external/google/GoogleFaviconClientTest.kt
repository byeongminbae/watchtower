package kr.byeongmin.watchtower.external.google

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleFaviconClientTest {
    private val googleFaviconClient = GoogleFaviconClient()

    @Test
    fun `사이트 주소로 구글에서 제공하는 파비콘 주소를 얻을 수 있음`() {
        /** when */
        val faviconUrl = googleFaviconClient.buildFaviconUrl("http://naver.com")

        /** then */
        assertEquals("https://www.google.com/s2/favicons?domain=naver.com&sz=128", faviconUrl)
    }

    @Test
    fun `파비콘 주소에는 사이트의 호스트만 사용해 경로나 인증 정보를 노출하지 않음`() {
        /** when */
        val faviconUrl = googleFaviconClient.buildFaviconUrl(
            "  HTTPS://user:password@Sub.Example.co.kr:8080/reset?token=secret#frag "
        )

        /** then */
        assertEquals("https://www.google.com/s2/favicons?domain=Sub.Example.co.kr&sz=128", faviconUrl)
    }

    @Test
    fun `호스트를 알 수 없는 사이트의 파비콘 주소는 생성하지 않음`() {
        /** given */
        val siteUrls = listOf(
            "file:///etc/passwd",
            "javascript:alert(1)",
            "mailto:a@example.com",
            "not a url",
            "",
            "https:///path"
        )

        /** when */
        val faviconUrls = siteUrls.map { googleFaviconClient.buildFaviconUrl(it) }

        /** then */
        assertTrue(faviconUrls.all { it == null })
    }

    @Test
    fun `호스트에 사용할 수 없는 문자가 있으면 파비콘 주소를 생성하지 않음`() {
        /** when */
        val faviconUrls = listOf("http://exa mple.com", "http://exa<mple.com").map {
            googleFaviconClient.buildFaviconUrl(it)
        }

        /** then */
        assertTrue(faviconUrls.all { it == null })
    }

    @Test
    fun `저장 가능한 길이를 넘는 파비콘 주소는 생성하지 않음`() {
        /** given */
        val longHost = "a".repeat(255) + ".com"

        /** when */
        val faviconUrl = googleFaviconClient.buildFaviconUrl("https://$longHost/")

        /** then */
        assertNull(faviconUrl)
    }
}
