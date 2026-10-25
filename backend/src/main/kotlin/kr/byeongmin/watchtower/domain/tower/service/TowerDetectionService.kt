package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.tower.accessor.TowerConditionAccessor
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.dns.DnsResolver
import kr.byeongmin.watchtower.global.utils.isNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TowerDetectionService(
    private val towerConditionAccessor: TowerConditionAccessor,
    private val dnsResolver: DnsResolver
) {
    fun isExternalAddress(url: TowerUrl): Boolean {
        return url.isExternalAddress(dnsResolver)
    }

    fun isAccessible(httpStatusCode: Int, currentHtml: String): Boolean {
        return httpStatusCode !in 400..499 && currentHtml.isNotBlank()
    }

    fun isFirstRequest(previousHtml: String?): Boolean {
        return previousHtml.isNull()
    }

    @Transactional(readOnly = true)
    fun isChanged(towerId: Long, previousHtml: String, currentHtml: String): Boolean {
        return towerConditionAccessor.getByTowerId(towerId).any {
            it.isSatisfied(previousHtml = previousHtml, currentHtml = currentHtml)
        }
    }
}
