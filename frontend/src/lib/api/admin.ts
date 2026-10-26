import { apiClient } from "./client";
import { isAdminMember, isAuthStats, isMemberStats, isTowerStats, isWatch, offsetOf } from "./guards";
import type { MemberRole } from "@/types/domain";
import type { WatchStatus } from "@/types/tower";
import type { SearchCriteria } from "@/types/pagination";

function params(options?: SearchCriteria & { readonly page: number }): string {
  return new URLSearchParams(options === undefined ? {} : {
    query: options.query, page: String(options.page), size: String(options.size),
    sortField: options.sortField, sortDirection: options.sortDirection
  }).toString();
}
export const adminApi = {
  getWatches: (options?: SearchCriteria & { readonly page: number }) => apiClient.get(`/bff/admin/towers?${params(options)}`, offsetOf(isWatch)),
  updateWatchStatus: (id: number, status: WatchStatus) => apiClient.put(`/bff/admin/towers/${id}`, { status }, isWatch),
  getMembers: (options?: SearchCriteria & { readonly page: number }) => apiClient.get(`/bff/admin/members?${params(options)}`, offsetOf(isAdminMember)),
  updateMemberRole: (id: number, role: MemberRole) => apiClient.put(`/bff/admin/members/${id}/roles`, { role }, isAdminMember),
  updateMemberStatus: (id: number, body: { readonly banned: boolean; readonly banReason: string }) => apiClient.put(`/bff/admin/members/${id}/status`, body, isAdminMember),
  getWatchStats: () => apiClient.get("/bff/admin/stats/towers", isTowerStats),
  getMemberStats: () => apiClient.get("/bff/admin/stats/members", isMemberStats),
  getAuthStats: () => apiClient.get("/bff/admin/stats/auth", isAuthStats)
};
