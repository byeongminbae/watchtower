import { rejectUnavailable } from "./availability";
import type { ApiEnvelope } from "./types";
import type { Member, MemberRole, MemberStatus, PaymentHistory, Watch, WatchStatus } from "@/types/domain";

export interface WatchStatsResult {
  totalActiveWatches: number;
  dailyTrend: Array<{ date: string; count: number }>;
}

export interface MemberStatsResult {
  totalMembers: number;
  todayLogins: number;
  totalWithdrawals: number;
}

export interface PaymentStatsResult {
  thisMonthRevenue: number;
  thisMonthPaymentCount: number;
}

export interface AuthStatsResult {
  todaySignups: number;
  todayLogins: number;
}

export const adminApi = {
  getWatches: (query?: string) => {
    void query;
    return rejectUnavailable<ApiEnvelope<Watch[]>>("admin.getWatches");
  },

  updateWatchStatus: (watchId: number, status: WatchStatus) => {
    void watchId;
    void status;
    return rejectUnavailable<ApiEnvelope<Watch>>(
      "admin.updateWatchStatus",
    );
  },

  cancelPayment: (paymentId: number) => {
    void paymentId;
    return rejectUnavailable<ApiEnvelope<string>>("admin.cancelPayment");
  },

  getPayments: (query?: string) => {
    void query;
    return rejectUnavailable<ApiEnvelope<PaymentHistory[]>>(
      "admin.getPayments",
    );
  },

  getMembers: (query?: string) => {
    void query;
    return rejectUnavailable<ApiEnvelope<Member[]>>("admin.getMembers");
  },

  updateMemberRole: (memberId: number, role: MemberRole) => {
    void memberId;
    void role;
    return rejectUnavailable<ApiEnvelope<Member>>("admin.updateMemberRole");
  },

  updateMemberStatus: (memberId: number, status: MemberStatus) => {
    void memberId;
    void status;
    return rejectUnavailable<ApiEnvelope<Member>>(
      "admin.updateMemberStatus",
    );
  },

  getWatchStats: () =>
    rejectUnavailable<ApiEnvelope<WatchStatsResult>>("admin.getWatchStats"),

  getMemberStats: () =>
    rejectUnavailable<ApiEnvelope<MemberStatsResult>>("admin.getMemberStats"),

  getPaymentStats: () =>
    rejectUnavailable<ApiEnvelope<PaymentStatsResult>>(
      "admin.getPaymentStats",
    ),

  getAuthStats: () =>
    rejectUnavailable<ApiEnvelope<AuthStatsResult>>("admin.getAuthStats"),
};
