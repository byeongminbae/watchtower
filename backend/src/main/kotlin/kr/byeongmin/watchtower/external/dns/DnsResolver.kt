package kr.byeongmin.watchtower.external.dns

import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.UnknownHostException

/** DNS 주소에 등록된 IP 주소를 조회하는 클래스 */
@Component
class DnsResolver {
    /** @return 조회에 실패하거나 시큐리티 이슈로 실패시 빈 리스트 */
    fun resolveAll(host: String): List<InetAddress> {
        return try {
            InetAddress.getAllByName(host).toList()
        } catch (_: UnknownHostException) {
            emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
