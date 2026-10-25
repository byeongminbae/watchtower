package kr.byeongmin.watchtower.external.google

import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class GoogleFaviconClient {
    /** @return 호스트를 추출할 수 없거나 생성 URL이 저장 길이 제한을 넘으면 null */
    fun buildFaviconUrl(url: String): String? {
        val host = try {
            UriComponentsBuilder.fromUriString(url.trim()).build().host
        } catch (_: IllegalArgumentException) {
            return null
        }

        if (host.isNullOrBlank()) {
            return null
        }

        val faviconSizePixels = 128
        val maxFaviconUrlLength = 255
        return UriComponentsBuilder.fromUriString("https://www.google.com/s2/favicons")
            .queryParam("domain", host)
            .queryParam("sz", faviconSizePixels)
            .build()
            .encode()
            .toUriString()
            .takeIf { it.length <= maxFaviconUrlLength }
    }
}
