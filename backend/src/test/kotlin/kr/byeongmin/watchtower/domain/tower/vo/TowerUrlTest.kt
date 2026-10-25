package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.external.dns.DnsResolver
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import tools.jackson.databind.exc.ValueInstantiationException
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetAddress
import kotlin.test.*

class TowerUrlTest {
    private val mapper = jacksonObjectMapper()
    private val dnsResolver = mock<DnsResolver>()
    private val url = TowerUrl.from("https://example.com")

    @Test
    fun `외부 IPv4와 IPv6 주소는 감시 대상으로 사용할 수 있음`() {
        /** given */
        whenever(dnsResolver.resolveAll(url.host)).thenReturn(
            listOf(InetAddress.ofLiteral("8.8.8.8"), InetAddress.ofLiteral("2001:4860:4860::8888"))
        )

        /** when */
        val result = url.isExternalAddress(dnsResolver)

        /** then */
        assertTrue(result)
    }

    @Test
    fun `감시 대상에 내부망이나 멀티캐스트 주소가 하나라도 있으면 거부함`() {
        listOf(
            "0.0.0.0",
            "127.0.0.1",
            "10.0.0.1",
            "172.16.0.1",
            "192.168.0.1",
            "169.254.169.254",
            "224.0.0.1",
            "::",
            "::1",
            "fe80::1",
            "fec0::1",
            "ff02::1"
        ).forEach { blockedAddress ->
            /** given */
            whenever(dnsResolver.resolveAll(url.host)).thenReturn(
                listOf(InetAddress.ofLiteral("8.8.8.8"), InetAddress.ofLiteral(blockedAddress))
            )

            /** when */
            val result = url.isExternalAddress(dnsResolver)

            /** then */
            assertFalse(result, blockedAddress)
        }
    }

    @Test
    fun `IP를 확인할 수 없는 사이트는 감시 대상으로 허용하지 않음`() {
        /** given */
        whenever(dnsResolver.resolveAll(url.host)).thenReturn(emptyList())

        /** when */
        val result = url.isExternalAddress(dnsResolver)

        /** then */
        assertFalse(result)
    }

    @Test
    fun `내부 주소를 감시하려 하면 허용되지 않은 URL이라는 오류가 발생함`() {
        /** given */
        whenever(dnsResolver.resolveAll(url.host)).thenReturn(listOf(InetAddress.ofLiteral("127.0.0.1")))

        /** when */
        val exception = assertFailsWith<BusinessException> {
            url.validateIsExternalIp(dnsResolver)
        }

        /** then */
        assertEquals(TowerError.URL_NOT_ALLOWED, exception.error)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "example.com",
            "/path",
            "ftp://example.com",
            "file:///etc/passwd",
            "https:///path",
            "httpsexample.com",
        ]
    )
    fun `HTTP 또는 HTTPS 절대 주소가 아니면 감시 대상 URL로 사용할 수 없음`(value: String) {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            TowerUrl.from(value)
        }
        val deserializationException = assertFailsWith<ValueInstantiationException> {
            mapper.readValue(mapper.writeValueAsBytes(value), TowerUrl::class.java)
        }

        /** then */
        assertEquals(CommonError.INVALID_INPUT_VALUE, exception.error)
        assertEquals(
            CommonError.INVALID_INPUT_VALUE,
            assertIs<BusinessException>(deserializationException.cause).error
        )
    }
}
