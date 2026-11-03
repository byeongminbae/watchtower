package kr.byeongmin.watchtower.global.exception

import kr.byeongmin.watchtower.external.slack.client.SlackClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertNull

class UnhandledExceptionAlertResolverTest {
    private val slackClient = mock<SlackClient>()
    private val resolver = UnhandledExceptionAlertResolver(slackClient)

    @Test
    fun `처리하지 못한 예외는 운영자에게 알리고 응답은 기본 오류 처리에 맡김`() {
        /** given */
        val request = MockHttpServletRequest("GET", "/api/v1/towers")
        val exception = IllegalStateException("DB 연결 끊김")

        /** when */
        val result = resolver.resolveException(request, MockHttpServletResponse(), null, exception)

        /** then */
        assertNull(result)
        verify(slackClient).sendErrorAlert(argThat { contains("GET /api/v1/towers") }, eq(exception))
    }
}
