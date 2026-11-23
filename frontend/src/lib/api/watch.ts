import { apiClient } from "./client";
import { arrayOf, cursorOf, isCondition, isHotUrl, isSnapshot, isWatch } from "./guards";
import type { SearchCriteria } from "@/types/pagination";
import type { WatchConditionInput, WatchDetection } from "@/types/tower";

export interface CreateWatchRequest extends WatchDetection {
  readonly name: string;
  readonly url: string;
  readonly fetchInterval: string;
  readonly includeInStat: boolean;
  readonly conditions: readonly WatchConditionInput[];
}
export interface UpdateWatchRequest extends WatchDetection {
  readonly name: string;
  readonly fetchInterval: string;
  readonly includeInStat: boolean;
}
export type CreateWatchConditionRequest = WatchConditionInput;
export type UpdateWatchConditionRequest = WatchConditionInput;
const basePath = "/bff/towers";
export const watchApi = {
  listMemberWatches: (memberId: number, options: Partial<SearchCriteria> & { readonly cursorId?: number; readonly cursorValue?: string } = {}) => {
    const query = new URLSearchParams({ size: String(options.size ?? 20), sortField: options.sortField ?? "ID", sortDirection: options.sortDirection ?? "DESC" });
    if (options.cursorId !== undefined) query.set("cursorId", String(options.cursorId));
    if (options.cursorValue !== undefined) query.set("cursorValue", options.cursorValue);
    if (options.query) query.set("query", options.query);
    return apiClient.get(`/bff/member/${memberId}/towers?${query}`, cursorOf(isWatch));
  },
  createWatch: (body: CreateWatchRequest) => apiClient.postEmpty(basePath, body),
  getTrendingWatches: () => apiClient.get(`${basePath}/trending`, arrayOf(isHotUrl)),
  getWatch: (watchId: number) => apiClient.get(`${basePath}/${watchId}`, isWatch),
  updateWatch: (watchId: number, body: UpdateWatchRequest) => apiClient.put(`${basePath}/${watchId}`, body, isWatch),
  deleteWatch: (watchId: number) => apiClient.deleteEmpty(`${basePath}/${watchId}`),
  retryCrawl: (watchId: number) => apiClient.post(`${basePath}/${watchId}/crawl-retry`, undefined, isWatch),
  pauseWatch: (watchId: number) => apiClient.post(`${basePath}/${watchId}/pause`, undefined, isWatch),
  resumeWatch: (watchId: number) => apiClient.post(`${basePath}/${watchId}/resume`, undefined, isWatch),
  getWatchConditions: (watchId: number) => apiClient.get(`${basePath}/${watchId}/conditions`, arrayOf(isCondition)),
  createWatchCondition: (watchId: number, body: CreateWatchConditionRequest) => apiClient.postEmpty(`${basePath}/${watchId}/conditions`, body),
  updateWatchCondition: (watchId: number, conditionId: number, body: UpdateWatchConditionRequest) => apiClient.put(`${basePath}/${watchId}/conditions/${conditionId}`, body, isCondition),
  deleteWatchCondition: (watchId: number, conditionId: number) => apiClient.deleteEmpty(`${basePath}/${watchId}/conditions/${conditionId}`),
  listWatchSnapshots: (watchId: number) => apiClient.get(`${basePath}/${watchId}/snapshots`, arrayOf(isSnapshot))
};
