package kr.byeongmin.watchtower.domain.tower.vo

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import kr.byeongmin.watchtower.domain.tower.error.TowerError
import kr.byeongmin.watchtower.external.dns.DnsResolver
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import java.net.URI

/**
 * HTTP(S) 스킴과 호스트 형식을 검증한 감시 대상 URL. 외부 주소 여부는 별도로 검사해야함
 *
 * http:// 또는 https:// 스킴과 호스트를 가진 절대 URL 만 허용함
 * DTO 에서 JSON 상으로는 중첩 객체가 아닌 평범한 문자열로 평탄화하여 주고받음
 *
 * @property [value] 팩토리 메서드에서 앞뒤 공백이 제거된 URL 문자열.
 *
 * 직렬화 시에는 [value] 가 그대로 문자열로 나가고(@JsonValue), 역직렬화 시에는 [from] 이 문자열을 검증해 인스턴스를 만듦(@JsonCreator)
 * @property [host] 생성 시 스킴/호스트 존재를 검증했으므로 항상 값이 있는 것이 보장됨
 */
class TowerUrl
// Jackson 의 creator 탐색 대상에서 이 생성자를 제외함
// 없으면 jackson-module-kotlin 이 이 생성자를 기본 creator 로 골라 from() 의 검증을 거치지 않고 직접 호출함
@JsonCreator(mode = JsonCreator.Mode.DISABLED)
private constructor(
    @get:JsonValue
    val value: String
) {
    val host: String = URI(value).host

    /**
     * 현재 DNS 조회 결과가 비어 있거나 내부 주소를 하나라도 포함하면 거부함
     * 조회 후 실제 접속까지 주소를 고정하지 않으므로 이 검사만으로 DNS rebinding을 막지는 못함
     */
    // TODO: 크롤러는 무조건 내부망과 별도로 격리된 네트워크에 분리해야 함
    //  애초에 앱단위에서 검증하더라도 원자적이지 않아서 검증 <-> 요청 사이에 DNS rebinding 이 성공할 수도 있음.
    fun validateIsExternalIp(dnsResolver: DnsResolver) {
        if (!isExternalAddress(dnsResolver)) {
            throw BusinessException(TowerError.URL_NOT_ALLOWED)
        }
    }

    fun isExternalAddress(dnsResolver: DnsResolver): Boolean {
        val resolvedAddresses = dnsResolver.resolveAll(host)
        return resolvedAddresses.isNotEmpty() && resolvedAddresses.none {
            it.isAnyLocalAddress ||
                it.isLoopbackAddress ||
                it.isSiteLocalAddress ||
                it.isLinkLocalAddress ||
                it.isMulticastAddress
        }
    }

    /** data class 를 쓰고 싶지만 .copy() 메서드가 자동 생성되어 오류남 */
    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return other is TowerUrl && value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    companion object {
        /**
         * 공백을 제거하고 http/https 스킴인지 host 가 null 이거나 빈 문자열은 아닌지만 검증
         * 테스트에서도 생성하고 사용해야기도 하고 DnsResolver 를 주입받기도 어려워 보여서 내부망 검사는 여기서 하지 않음
         */
        @JvmStatic
        @JsonCreator
        fun from(url: String): TowerUrl {
            val trimmedUrl = url.trim()

            val url = runCatching { URI(trimmedUrl) }.getOrNull()
                ?: throw BusinessException(CommonError.INVALID_INPUT_VALUE)

            if (!validateIsHttpScheme(url) || url.host.isNullOrBlank()) {
                throw BusinessException(CommonError.INVALID_INPUT_VALUE)
            }

            return TowerUrl(trimmedUrl)
        }

        private fun validateIsHttpScheme(url: URI): Boolean {
            return url.scheme.equals("http", ignoreCase = true) ||
                    url.scheme.equals("https", ignoreCase = true)
        }
    }
}
