import { rejectUnavailable } from "./availability";
import { apiClient } from "./client";
import type { ApiEnvelope } from "./types";
import type { Member, PaymentHistory, Subscription, Watch } from "@/types/domain";

function isMember(value: unknown): value is Member {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value) &&
    "id" in value &&
    typeof value.id === "number" &&
    Number.isSafeInteger(value.id) &&
    value.id > 0 &&
    "nickname" in value &&
    typeof value.nickname === "string" &&
    "email" in value &&
    typeof value.email === "string" &&
    "profileImageUrl" in value &&
    typeof value.profileImageUrl === "string" &&
    "lastSignInAt" in value &&
    typeof value.lastSignInAt === "string"
  );
}

export const memberApi = {
  getMember: (memberId: number) =>
    apiClient.get(`/api/v1/member/${memberId}`, isMember),

  updateMember: (
    memberId: number,
    body: Partial<Pick<Member, "nickname" | "profileImageUrl">>,
  ) => {
    void memberId;
    void body;
    return rejectUnavailable<ApiEnvelope<Member>>("member.updateMember");
  },

  deleteMember: (memberId: number) => {
    void memberId;
    return rejectUnavailable<ApiEnvelope<string>>("member.deleteMember");
  },

  getMemberWatches: (memberId: number) => {
    void memberId;
    return rejectUnavailable<ApiEnvelope<Watch[]>>(
      "member.getMemberWatches",
    );
  },

  getMemberPaymentHistories: (memberId: number) => {
    void memberId;
    return rejectUnavailable<ApiEnvelope<PaymentHistory[]>>(
      "member.getMemberPaymentHistories",
    );
  },

  getMemberSubscription: (memberId: number) => {
    void memberId;
    return rejectUnavailable<ApiEnvelope<Subscription>>(
      "member.getMemberSubscription",
    );
  },

  cancelMemberSubscription: (memberId: number) => {
    void memberId;
    return rejectUnavailable<ApiEnvelope<string>>(
      "member.cancelMemberSubscription",
    );
  },
};
