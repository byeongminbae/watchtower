package kr.byeongmin.watchtower.domain.tower.service

import kr.byeongmin.watchtower.domain.tower.accessor.HotTowerAccessor
import kr.byeongmin.watchtower.domain.tower.dto.HotTowerResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HotTowerServiceTest {
    private val hotTowerAccessor = mock<HotTowerAccessor>()

    private val hotTowerService = HotTowerService(
        hotTowerAccessor = hotTowerAccessor
    )

    @Test
    fun `인기 타워 목록에서 사이트 주소와 변경 감지 횟수를 확인할 수 있음`() {
        /** given */
        val hotTowerResponseDtos = listOf(
            HotTowerResponseDto(
                domain = "a.com",
                url = "https://a.com",
                changeCount = 12
            ),
            HotTowerResponseDto(
                domain = "b.com",
                url = "http://b.com:8080",
                changeCount = 7
            )
        )
        whenever(hotTowerAccessor.findAll()).thenReturn(hotTowerResponseDtos)

        /** when */
        val result = hotTowerService.getTrendingTowers()

        /** then */
        assertEquals(hotTowerResponseDtos, result.data)
    }

    @Test
    fun `집계된 인기 타워가 없으면 목록은 비어 있음`() {
        /** given */
        whenever(hotTowerAccessor.findAll()).thenReturn(emptyList())

        /** when */
        val result = hotTowerService.getTrendingTowers()

        /** then */
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `인기 타워 목록을 갱신하면 집계된 조회 데이터도 갱신함`() {
        /** when */
        hotTowerService.refreshHotTowers()

        /** then */
        verify(hotTowerAccessor).refresh()
    }
}
