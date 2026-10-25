package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.tower.accessor.TowerConditionAccessor
import kr.byeongmin.watchtower.domain.tower.entity.TowerCondition
import kr.byeongmin.watchtower.domain.tower.vo.TowerUrl
import kr.byeongmin.watchtower.external.dns.DnsResolver
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TowerDetectionServiceTest {
    private val conditionAccessor = mock<TowerConditionAccessor>()
    private val dnsResolver = mock<DnsResolver>()
    private val service = TowerDetectionService(conditionAccessor, dnsResolver)
    private val url = mock<TowerUrl>()

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `감시 대상의 외부 주소 여부는 URL 검사 결과를 사용함`(external: Boolean) {
        /** given */
        whenever(url.isExternalAddress(dnsResolver)).thenReturn(external)

        /** when */
        val result = service.isExternalAddress(url)

        /** then */
        assertEquals(external, result)
    }

    @Test
    fun `4xx 응답이거나 본문이 비어 있는 페이지는 수집 가능한 페이지로 보지 않음`() {
        /** when */
        val success = service.isAccessible(200, "<p>본문</p>")
        val clientError = service.isAccessible(400, "<p>오류</p>")
        val lastClientError = service.isAccessible(499, "<p>오류</p>")
        val blankHtml = service.isAccessible(200, "  ")

        /** then */
        assertTrue(success)
        assertFalse(clientError)
        assertFalse(lastClientError)
        assertFalse(blankHtml)
    }

    @Test
    fun `이전에 수집한 본문이 없을 때만 최초 수집으로 판단함`() {
        /** when */
        val missingHtml = service.isFirstRequest(null)
        val emptyHtml = service.isFirstRequest("")
        val previousHtml = service.isFirstRequest("<p>이전</p>")

        /** then */
        assertTrue(missingHtml)
        assertFalse(emptyHtml)
        assertFalse(previousHtml)
    }

    @Test
    fun `타워에 감지 조건이 없으면 변경이 없는 것으로 판단함`() {
        /** given */
        whenever(conditionAccessor.getByTowerId(7L)).thenReturn(emptyList())

        /** when */
        val result = service.isChanged(7L, "이전", "현재")

        /** then */
        assertFalse(result)
    }

    @Test
    fun `감지 조건을 하나도 만족하지 않으면 변경이 없는 것으로 판단함`() {
        /** given */
        val condition = mock<TowerCondition>()
        whenever(conditionAccessor.getByTowerId(7L)).thenReturn(listOf(condition))
        whenever(condition.isSatisfied("이전", "현재")).thenReturn(false)

        /** when */
        val result = service.isChanged(7L, "이전", "현재")

        /** then */
        assertFalse(result)
    }

    @Test
    fun `감지 조건 중 하나만 만족해도 변경이 있는 것으로 판단함`() {
        /** given */
        val first = mock<TowerCondition>()
        val matched = mock<TowerCondition>()
        whenever(conditionAccessor.getByTowerId(7L)).thenReturn(listOf(first, matched))
        whenever(first.isSatisfied("이전", "현재")).thenReturn(false)
        whenever(matched.isSatisfied("이전", "현재")).thenReturn(true)

        /** when */
        val result = service.isChanged(7L, "이전", "현재")

        /** then */
        assertTrue(result)
    }
}
