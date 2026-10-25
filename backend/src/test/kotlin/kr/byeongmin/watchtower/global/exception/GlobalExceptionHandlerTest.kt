package kr.byeongmin.watchtower.global.exception

import kr.byeongmin.watchtower.global.error.CommonError
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

class GlobalExceptionHandlerTest {
    @RestController
    private class FailingController(private val exception: BusinessException) {
        @GetMapping("/test/business-error")
        fun fail(): Nothing {
            throw exception
        }
    }

    @Test
    fun `비즈니스 오류는 HTTP 200 응답으로 나가고 실패 상태와 오류 정보로 구분함`() {
        /** given */
        val timestamp = LocalDateTime.of(2026, 10, 20, 12, 0)
        val mvc = MockMvcBuilders.standaloneSetup(
            FailingController(BusinessException(CommonError.RESOURCE_NOT_FOUND, timestamp))
        )
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

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
}
