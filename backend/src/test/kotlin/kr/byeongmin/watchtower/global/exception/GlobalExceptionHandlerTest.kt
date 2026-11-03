package kr.byeongmin.watchtower.global.exception

import kr.byeongmin.watchtower.external.slack.client.SlackClient
import kr.byeongmin.watchtower.global.error.CommonError
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

class GlobalExceptionHandlerTest {
    private val slackClient = mock<SlackClient>()

    @RestController
    private class FailingController(private val exception: BusinessException) {
        @GetMapping("/test/business-error")
        fun fail(): Nothing {
            throw exception
        }
    }

    private fun mockMvcThrowing(exception: BusinessException): MockMvc {
        return MockMvcBuilders.standaloneSetup(FailingController(exception))
            .setControllerAdvice(GlobalExceptionHandler(slackClient))
            .build()
    }

    @Test
    fun `비즈니스 오류는 HTTP 200 응답으로 나가고 실패 상태와 오류 정보로 구분함`() {
        /** given */
        val timestamp = LocalDateTime.of(2026, 10, 20, 12, 0)
        val mvc = mockMvcThrowing(BusinessException(CommonError.RESOURCE_NOT_FOUND, timestamp))

        /** when */
        val result = mvc
            .perform(get("/test/business-error"))

        /** then */
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.statusCode").value(CommonError.RESOURCE_NOT_FOUND.statusCode))
            .andExpect(jsonPath("$.message").value(CommonError.RESOURCE_NOT_FOUND.message))
            .andExpect(jsonPath("$.timestamp").value("2026-10-20T12:00:00"))
    }

    @Test
    fun `서버 쪽 문제인 비즈니스 오류는 운영자에게 알림을 보냄`() {
        /** given */
        val exception = BusinessException(CommonError.EXTERNAL_API_ERROR)
        val mvc = mockMvcThrowing(exception)

        /** when */
        mvc.perform(get("/test/business-error"))

        /** then */
        verify(slackClient).sendErrorAlert(argThat { contains(CommonError.EXTERNAL_API_ERROR.statusCode) }, eq(exception))
    }

    @Test
    fun `사용자 요청 때문에 생긴 비즈니스 오류는 운영자에게 알리지 않음`() {
        /** given */
        val mvc = mockMvcThrowing(BusinessException(CommonError.RESOURCE_NOT_FOUND))

        /** when */
        mvc.perform(get("/test/business-error"))

        /** then */
        verifyNoInteractions(slackClient)
    }
}
