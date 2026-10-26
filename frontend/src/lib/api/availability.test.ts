import { beforeEach, expect, it, vi } from "vitest";
import { apiClient } from "./client";
import { memberApi } from "./member";
import { watchApi } from "./watch";
import { adminApi } from "./admin";
import { notificationApi } from "./notification";
import { defaultSearch } from "@/types/pagination";

vi.mock("./client", () => ({ apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn(), postEmpty: vi.fn(), deleteEmpty: vi.fn() } }));
beforeEach(() => vi.clearAllMocks());
it("회원 수정과 탈퇴는 실제 회원 경로와 HTTP 방식으로 연결될 것이다", () => {
  // given
  const profile = { nickname: "새 이름", profileImageUrl: "https://example.com/avatar.png" };
  // when
  memberApi.updateMember(7, profile); memberApi.deleteMember(7);
  // then
  expect(apiClient.put).toHaveBeenCalledWith("/bff/member/7", profile, expect.any(Function));
  expect(apiClient.deleteEmpty).toHaveBeenCalledWith("/bff/member/7");
});
it("타워 생성과 수정은 Duration과 모든 필수 필드를 전송할 것이다", () => {
  // given
  const create = { name: "관찰", url: "https://example.com", fetchInterval: "PT5M", includeInStat: true, notificationChannelIds: [3],
    conditions: [{ type: "KEYWORD", keyword: "마감", isSatisfiedAtAppearance: false }] } as const;
  const update = { name: "관찰", fetchInterval: "PT10M", includeInStat: false, status: "PAUSED" } as const;
  // when
  watchApi.createWatch(create); watchApi.updateWatch(9, update);
  // then
  expect(apiClient.postEmpty).toHaveBeenCalledWith("/bff/towers", create);
  expect(apiClient.put).toHaveBeenCalledWith("/bff/towers/9", update, expect.any(Function));
});
it("조건 수정과 삭제는 타워에 속한 조건 경로를 사용할 것이다", () => {
  // given
  const input = { type: "REGEX", regex: "sale.*", isSatisfiedAtAppearance: true } as const;
  // when
  watchApi.createWatchCondition(9, input); watchApi.updateWatchCondition(9, 3, input); watchApi.deleteWatchCondition(9, 3);
  // then
  expect(apiClient.postEmpty).toHaveBeenCalledWith("/bff/towers/9/conditions", input);
  expect(apiClient.put).toHaveBeenCalledWith("/bff/towers/9/conditions/3", input, expect.any(Function));
  expect(apiClient.deleteEmpty).toHaveBeenCalledWith("/bff/towers/9/conditions/3");
});
it("목록의 커서 값과 검색 및 정렬은 서버에 전달될 것이다", () => {
  // given
  const criteria = { ...defaultSearch, query: "공고 &", sortField: "CREATED_AT", cursorId: 5, cursorValue: "2026-10-25T10:00:00" } as const;
  // when
  watchApi.listMemberWatches(7, criteria); notificationApi.listChannels(criteria);
  // then
  const calls = vi.mocked(apiClient.get).mock.calls.map(call => new URL(call[0], "https://front.example"));
  for (const url of calls) {
    expect(url.searchParams.get("query")).toBe("공고 &");
    expect(url.searchParams.get("cursorValue")).toBe(criteria.cursorValue);
    expect(url.searchParams.get("sortField")).toBe("CREATED_AT");
  }
});
it("관리자 정지는 불리언과 사유를 함께 전송하고 권한은 별도 경로로 바꿀 것이다", () => {
  // given
  const status = { banned: true, banReason: "반복적인 정책 위반" };
  // when
  adminApi.updateMemberStatus(7, status); adminApi.updateMemberRole(7, "ADMIN");
  // then
  expect(apiClient.put).toHaveBeenCalledWith("/bff/admin/members/7/status", status, expect.any(Function));
  expect(apiClient.put).toHaveBeenCalledWith("/bff/admin/members/7/roles", { role: "ADMIN" }, expect.any(Function));
});
it("관리자 목록은 오프셋 페이지와 서버 검색을 요청할 것이다", () => {
  // given
  const criteria = { ...defaultSearch, query: "회원", page: 2 };
  // when
  adminApi.getMembers(criteria);
  // then
  const url = new URL(vi.mocked(apiClient.get).mock.calls[0]?.[0] ?? "", "https://front.example");
  expect(url.pathname).toBe("/bff/admin/members");
  expect(url.searchParams.get("page")).toBe("2");
  expect(url.searchParams.get("query")).toBe("회원");
});
it("알림 채널 수정에서는 생성할 때 정한 유형을 보내지 않을 것이다", () => {
  // given
  const update = { name: "알림", webhookUrl: "https://example.com/hook" };
  // when
  notificationApi.updateChannel(3, update);
  // then
  expect(apiClient.put).toHaveBeenCalledWith("/bff/notification/channels/3", update, expect.any(Function));
});
