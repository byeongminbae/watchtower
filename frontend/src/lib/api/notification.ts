import { apiClient } from "./client";
import type { SearchCriteria } from "@/types/pagination";
import type { NotificationChannel, NotificationChannelPage, NotificationChannelCreate, NotificationChannelUpdate } from "@/types/notification";

function isChannel(value: unknown): value is NotificationChannel {
  return typeof value === "object" && value !== null &&
    "id" in value && typeof value.id === "number" && Number.isSafeInteger(value.id) && value.id > 0 &&
    "name" in value && typeof value.name === "string" &&
    "type" in value && (value.type === "DISCORD" || value.type === "TELEGRAM") &&
    "webhookUrl" in value && typeof value.webhookUrl === "string";
}

function isChannelPage(value: unknown): value is NotificationChannelPage {
  return typeof value === "object" && value !== null &&
    "data" in value && Array.isArray(value.data) && value.data.every(isChannel) &&
    "size" in value && typeof value.size === "number" &&
    "nextCursorId" in value && (value.nextCursorId === null || typeof value.nextCursorId === "number") &&
    "nextCursorValue" in value && (value.nextCursorValue === null || typeof value.nextCursorValue === "string") &&
    "hasNext" in value && typeof value.hasNext === "boolean";
}

const path = "/bff/notification/channels";

export const notificationApi = {
  listChannels: (options: Partial<SearchCriteria> & { readonly cursorId?: number; readonly cursorValue?: string } = {}) => {
    const query = new URLSearchParams({ sortField: options.sortField ?? "ID", sortDirection: options.sortDirection ?? "DESC", size: String(options.size ?? 20) });
    if (options.cursorId !== undefined) query.set("cursorId", String(options.cursorId));
    if (options.cursorValue !== undefined) query.set("cursorValue", options.cursorValue);
    if (options.query !== undefined) query.set("query", options.query);
    return apiClient.get(`${path}?${query}`, isChannelPage);
  },
  getChannel: (id: number) => apiClient.get(`${path}/${id}`, isChannel),
  createChannel: (body: NotificationChannelCreate) => apiClient.post(path, body, isChannel),
  updateChannel: (id: number, body: NotificationChannelUpdate) => apiClient.put(`${path}/${id}`, body, isChannel),
  deleteChannel: (id: number) => apiClient.deleteEmpty(`${path}/${id}`),
};
