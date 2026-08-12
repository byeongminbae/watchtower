import { rejectUnavailable } from "./availability";
import type { ApiEnvelope } from "./types";
import type { PaymentHistory } from "@/types/domain";

export interface ConfirmTossPaymentRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export const paymentApi = {
  getPayment: (paymentId: number) => {
    void paymentId;
    return rejectUnavailable<ApiEnvelope<PaymentHistory>>(
      "payment.getPayment",
    );
  },

  confirmTossPayment: (body: ConfirmTossPaymentRequest) => {
    void body;
    return rejectUnavailable<ApiEnvelope<PaymentHistory>>(
      "payment.confirmTossPayment",
    );
  },
};
