import { rejectUnavailable } from "./availability";
import type { ApiEnvelope } from "./types";
import type { Member, PaymentHistory, Subscription, Watch } from "@/types/domain";

export const memberApi = {
  getMember: (memberId: number) => {
    void memberId;
    return rejectUnavailable<ApiEnvelope<Member>>("member.getMember");
  },

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
