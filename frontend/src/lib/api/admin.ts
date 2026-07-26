import { apiClient } from "./client";
import { requireArrayData, requireObjectData } from "./types";
import type { ApiEnvelope } from "./types";
import type { Member, MemberRole, PaymentHistory, Watch, WatchStatus } from "@/types/domain";

// 통계 응답도 스텁 상태라, 대시보드에서 실제로 필요한 형태로 추론한다.
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
  // GET /api/v1/admin/watches?query=
  // 스웨거 스펙에는 없으나, "결제 내역 조회/검색"·"전체 유저 조회/검색"과 대칭되는
  // "전체 와치리스트 조회/검색" 엔드포인트가 관리자 기능상 반드시 필요하다고 추론해 채운다.
  getWatches: (query?: string) =>
    apiClient.get<ApiEnvelope<Watch[]>>(
      `/api/v1/admin/watches${query ? `?query=${encodeURIComponent(query)}` : ""}`,
    ).then(requireArrayData),

  // PATCH /api/v1/admin/watches/{watchId}
  updateWatchStatus: (watchId: number, status: WatchStatus) =>
    apiClient
      .patch<ApiEnvelope<Watch>>(`/api/v1/admin/watches/${watchId}`, { status })
      .then(requireObjectData),

  // POST /api/v1/admin/payments/{paymentId}/cancel
  cancelPayment: (paymentId: number) =>
    apiClient.post<ApiEnvelope<string>>(`/api/v1/admin/payments/${paymentId}/cancel`),

  // GET /api/v1/admin/payments?query=
  getPayments: (query?: string) =>
    apiClient.get<ApiEnvelope<PaymentHistory[]>>(
      `/api/v1/admin/payments${query ? `?query=${encodeURIComponent(query)}` : ""}`,
    ).then(requireArrayData),

  // GET /api/v1/admin/users?query=
  getUsers: (query?: string) =>
    apiClient.get<ApiEnvelope<Member[]>>(
      `/api/v1/admin/users${query ? `?query=${encodeURIComponent(query)}` : ""}`,
    ).then(requireArrayData),

  // PATCH /api/v1/admin/users/{userId}/roles
  updateUserRole: (userId: number, role: MemberRole) =>
    apiClient
      .patch<ApiEnvelope<Member>>(`/api/v1/admin/users/${userId}/roles`, { role })
      .then(requireObjectData),

  // PATCH /api/v1/admin/users/{userId}/status (ban/unban)
  updateUserStatus: (userId: number, banned: boolean) =>
    apiClient
      .patch<ApiEnvelope<Member>>(`/api/v1/admin/users/${userId}/status`, { banned })
      .then(requireObjectData),

  // GET /api/v1/admin/stats/watches
  getWatchStats: () =>
    apiClient
      .get<ApiEnvelope<WatchStatsResult>>("/api/v1/admin/stats/watches")
      .then(requireObjectData),

  // GET /api/v1/admin/stats/users
  getUserStats: () =>
    apiClient
      .get<ApiEnvelope<UserStatsResult>>("/api/v1/admin/stats/users")
      .then(requireObjectData),

  // GET /api/v1/admin/stats/payments
  getPaymentStats: () =>
    apiClient
      .get<ApiEnvelope<PaymentStatsResult>>("/api/v1/admin/stats/payments")
      .then(requireObjectData),

  // GET /api/v1/admin/stats/auth
  getAuthStats: () =>
    apiClient
      .get<ApiEnvelope<AuthStatsResult>>("/api/v1/admin/stats/auth")
      .then(requireObjectData),
};
