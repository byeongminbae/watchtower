import { apiClient } from "./client";
import type { Member } from "@/types/domain";

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
    apiClient.get(`/bff/member/${memberId}`, isMember),

  updateMember: (
    memberId: number,
    body: Pick<Member, "nickname" | "profileImageUrl">,
  ) => apiClient.put(`/bff/member/${memberId}`, body, isMember),

  deleteMember: (memberId: number) =>
    apiClient.deleteEmpty(`/bff/member/${memberId}`),
};
