package kr.byeongmin.watchtower.domain.payment.controller

import io.swagger.v3.oas.annotations.Operation
import kr.byeongmin.watchtower.global.response.SuccessResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController {
    @Operation(summary = "결제 내역 단건 상세 조회")
    @GetMapping("/{paymentId}")
    fun getPayment(
        @PathVariable paymentId: Long
    ): SuccessResponse<String> {
        return SuccessResponse("")
    }

    @Operation(summary = "토스 결제 승인")
    @PostMapping("/toss/confirm")
    fun confirmTossPayment(): SuccessResponse<String> {
        return SuccessResponse("")
    }
}
