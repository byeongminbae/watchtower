import { rejectUnavailable } from "./availability";
import type { ApiEnvelope } from "./types";
import type { HotUrl, Watch, WatchCondition, WatchSnapshot } from "@/types/domain";

export interface CreateWatchRequest {
  name: string;
  url: string;
  intervalSeconds: number;
  includeInStat: boolean;
  conditions: Array<Pick<WatchCondition, "type" | "keyword" | "regex">>;
}

export interface UpdateWatchRequest {
  name?: string;
  intervalSeconds?: number;
  includeInStat?: boolean;
  status?: "RUNNING" | "PAUSED";
}

export type CreateWatchConditionRequest = Pick<WatchCondition, "type" | "keyword" | "regex">;
export type UpdateWatchConditionRequest = Partial<CreateWatchConditionRequest>;

export const watchApi = {
  createWatch: (body: CreateWatchRequest) => {
    void body;
    return rejectUnavailable<ApiEnvelope<Watch>>("watch.createWatch");
  },

  getTrendingWatches: () =>
    rejectUnavailable<ApiEnvelope<HotUrl[]>>("watch.getTrendingWatches"),

  getWatch: (watchId: number) => {
    void watchId;
    return rejectUnavailable<ApiEnvelope<Watch>>("watch.getWatch");
  },

  updateWatch: (watchId: number, body: UpdateWatchRequest) => {
    void watchId;
    void body;
    return rejectUnavailable<ApiEnvelope<Watch>>("watch.updateWatch");
  },

  deleteWatch: (watchId: number) => {
    void watchId;
    return rejectUnavailable<ApiEnvelope<string>>("watch.deleteWatch");
  },

  sendTestNotification: (watchId: number) => {
    void watchId;
    return rejectUnavailable<ApiEnvelope<string>>(
      "watch.sendTestNotification",
    );
  },

  getWatchConditions: (watchId: number) => {
    void watchId;
    return rejectUnavailable<ApiEnvelope<WatchCondition[]>>(
      "watch.getWatchConditions",
    );
  },

  createWatchCondition: (
    watchId: number,
    body: CreateWatchConditionRequest,
  ) => {
    void watchId;
    void body;
    return rejectUnavailable<ApiEnvelope<WatchCondition>>(
      "watch.createWatchCondition",
    );
  },

  updateWatchCondition: (
    watchId: number,
    conditionId: number,
    body: UpdateWatchConditionRequest,
  ) => {
    void watchId;
    void conditionId;
    void body;
    return rejectUnavailable<ApiEnvelope<WatchCondition>>(
      "watch.updateWatchCondition",
    );
  },

  deleteWatchCondition: (watchId: number, conditionId: number) => {
    void watchId;
    void conditionId;
    return rejectUnavailable<ApiEnvelope<string>>(
      "watch.deleteWatchCondition",
    );
  },

  listWatchSnapshots: (watchId: number) => {
    void watchId;
    return rejectUnavailable<ApiEnvelope<WatchSnapshot[]>>(
      "watch.listWatchSnapshots",
    );
  },

  getWatchSnapshot: (watchId: number, snapshotId: number) => {
    void watchId;
    void snapshotId;
    return rejectUnavailable<ApiEnvelope<WatchSnapshot>>(
      "watch.getWatchSnapshot",
    );
  },
};
