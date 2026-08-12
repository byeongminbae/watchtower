import { rejectUnavailable } from "./availability";
import type { ApiEnvelope } from "./types";
import type { Member, MemberRole, PaymentHistory, Watch, WatchStatus } from "@/types/domain";

export interface WatchStatsResult {
  totalActiveWatches: number;
  dailyTrend: Array<{ date: string; count: number }>;
}

export interface UserStatsResult {
  totalUsers: number;
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

  getUsers: (query?: string) => {
    void query;
    return rejectUnavailable<ApiEnvelope<Member[]>>("admin.getUsers");
  },

  updateUserRole: (userId: number, role: MemberRole) => {
    void userId;
    void role;
    return rejectUnavailable<ApiEnvelope<Member>>("admin.updateUserRole");
  },

  updateUserStatus: (userId: number, banned: boolean) => {
    void userId;
    void banned;
    return rejectUnavailable<ApiEnvelope<Member>>(
      "admin.updateUserStatus",
    );
  },

  getWatchStats: () =>
    rejectUnavailable<ApiEnvelope<WatchStatsResult>>("admin.getWatchStats"),

  getUserStats: () =>
    rejectUnavailable<ApiEnvelope<UserStatsResult>>("admin.getUserStats"),

  getPaymentStats: () =>
    rejectUnavailable<ApiEnvelope<PaymentStatsResult>>(
      "admin.getPaymentStats",
    ),

  getAuthStats: () =>
    rejectUnavailable<ApiEnvelope<AuthStatsResult>>("admin.getAuthStats"),
};
