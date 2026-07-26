import { apiClient } from "./client";
import { requireArrayData, requireObjectData } from "./types";
import type { ApiEnvelope } from "./types";
import type { HotUrl, Watch, WatchCondition, WatchSnapshot } from "@/types/domain";

// 스웨거에 요청 body 스펙이 비어있어(스텁 상태), README 기능 요구사항 기준으로 필드를 추론한다.
export interface CreateWatchRequest {
  name: string;
  url: string;
  intervalSeconds: number;
  includeInStat: boolean;
  // 최초 등록 시 조건 최소 1개는 있어야 한다고 추론 (없으면 감지할 기준이 없음)
  conditions: Array<Pick<WatchCondition, "type" | "keyword" | "regex">>;
}

export interface UpdateWatchRequest {
  name?: string;
  intervalSeconds?: number;
  includeInStat?: boolean;
  // 일시정지/재개는 PATCH로 status만 바꾸는 것으로 추론
  status?: "RUNNING" | "PAUSED";
}

export type CreateWatchConditionRequest = Pick<WatchCondition, "type" | "keyword" | "regex">;
export type UpdateWatchConditionRequest = Partial<CreateWatchConditionRequest>;

export const watchApi = {
  // POST /api/v1/watches
  createWatch: (body: CreateWatchRequest) =>
    apiClient.post<ApiEnvelope<Watch>>("/api/v1/watches", body).then(requireObjectData),

  // GET /api/v1/watches/trending
  getTrendingWatches: () =>
    apiClient.get<ApiEnvelope<HotUrl[]>>("/api/v1/watches/trending").then(requireArrayData),

  // GET /api/v1/watches/{watchId}
  getWatch: (watchId: number) =>
    apiClient.get<ApiEnvelope<Watch>>(`/api/v1/watches/${watchId}`).then(requireObjectData),

  // PATCH /api/v1/watches/{watchId}
  updateWatch: (watchId: number, body: UpdateWatchRequest) =>
    apiClient.patch<ApiEnvelope<Watch>>(`/api/v1/watches/${watchId}`, body).then(requireObjectData),

  // DELETE /api/v1/watches/{watchId}
  deleteWatch: (watchId: number) => apiClient.delete<ApiEnvelope<string>>(`/api/v1/watches/${watchId}`),

  // POST /api/v1/watches/{watchId}/notify
  sendTestNotification: (watchId: number) =>
    apiClient.post<ApiEnvelope<string>>(`/api/v1/watches/${watchId}/notify`),

  // GET /api/v1/watches/{watchId}/conditions
  getWatchConditions: (watchId: number) =>
    apiClient
      .get<ApiEnvelope<WatchCondition[]>>(`/api/v1/watches/${watchId}/conditions`)
      .then(requireArrayData),

  // POST /api/v1/watches/{watchId}/conditions
  createWatchCondition: (watchId: number, body: CreateWatchConditionRequest) =>
    apiClient
      .post<ApiEnvelope<WatchCondition>>(`/api/v1/watches/${watchId}/conditions`, body)
      .then(requireObjectData),

  // PATCH /api/v1/watches/{watchId}/conditions/{conditionId}
  updateWatchCondition: (watchId: number, conditionId: number, body: UpdateWatchConditionRequest) =>
    apiClient.patch<ApiEnvelope<WatchCondition>>(
      `/api/v1/watches/${watchId}/conditions/${conditionId}`,
      body,
    ).then(requireObjectData),

  // DELETE /api/v1/watches/{watchId}/conditions/{conditionId}
  deleteWatchCondition: (watchId: number, conditionId: number) =>
    apiClient.delete<ApiEnvelope<string>>(`/api/v1/watches/${watchId}/conditions/${conditionId}`),

  // GET /api/v1/watches/{watchId}/snapshots
  listWatchSnapshots: (watchId: number) =>
    apiClient
      .get<ApiEnvelope<WatchSnapshot[]>>(`/api/v1/watches/${watchId}/snapshots`)
      .then(requireArrayData),

  // GET /api/v1/watches/{watchId}/snapshots/{snapshotId}
  getWatchSnapshot: (watchId: number, snapshotId: number) =>
    apiClient
      .get<ApiEnvelope<WatchSnapshot>>(`/api/v1/watches/${watchId}/snapshots/${snapshotId}`)
      .then(requireObjectData),
};
