import { apiClient } from "./client";
import { ApiEnvelope } from "./types";
import { Member, PaymentHistory, Subscription, Watch } from "@/types/domain";

export const memberApi = {
  // GET /api/v1/member/{memberId}
  getMember: (memberId: number) => apiClient.get<ApiEnvelope<Member>>(`/api/v1/member/${memberId}`),

  // PATCH /api/v1/member/{memberId}
  updateMember: (memberId: number, body: Partial<Pick<Member, "nickname" | "profileImageUrl">>) =>
    apiClient.patch<ApiEnvelope<Member>>(`/api/v1/member/${memberId}`, body),

  // DELETE /api/v1/member/{memberId}
  deleteMember: (memberId: number) => apiClient.delete<ApiEnvelope<string>>(`/api/v1/member/${memberId}`),

  // GET /api/v1/member/{memberId}/watches
  getMemberWatches: (memberId: number) =>
    apiClient.get<ApiEnvelope<Watch[]>>(`/api/v1/member/${memberId}/watches`),

  // GET /api/v1/member/{memberId}/payments
  getMemberPaymentHistories: (memberId: number) =>
    apiClient.get<ApiEnvelope<PaymentHistory[]>>(`/api/v1/member/${memberId}/payments`),

  // GET /api/v1/member/{memberId}/subscriptions
  getMemberSubscription: (memberId: number) =>
    apiClient.get<ApiEnvelope<Subscription>>(`/api/v1/member/${memberId}/subscriptions`),

  // DELETE /api/v1/member/{memberId}/subscriptions
  cancelMemberSubscription: (memberId: number) =>
    apiClient.delete<ApiEnvelope<string>>(`/api/v1/member/${memberId}/subscriptions`),
};
