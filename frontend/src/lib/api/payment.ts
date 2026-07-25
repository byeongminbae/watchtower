import { apiClient } from "./client";
import { ApiEnvelope } from "./types";
import { PaymentHistory } from "@/types/domain";

// 토스 결제 승인 요청 바디는 토스 API 문서 기준 필드로 추론 (paymentKey, orderId, amount)
export interface ConfirmTossPaymentRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export const paymentApi = {
  // GET /api/v1/payments/{paymentId}
  getPayment: (paymentId: number) =>
    apiClient.get<ApiEnvelope<PaymentHistory>>(`/api/v1/payments/${paymentId}`),

  // POST /api/v1/payments/toss/confirm
  confirmTossPayment: (body: ConfirmTossPaymentRequest) =>
    apiClient.post<ApiEnvelope<PaymentHistory>>("/api/v1/payments/toss/confirm", body),
};
