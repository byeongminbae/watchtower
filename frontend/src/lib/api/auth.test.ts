import { afterEach, expect, it, vi } from "vitest";
import { authApi } from "./auth";

const timestamp = "2026-10-26T12:00:00";
afterEach(() => vi.restoreAllMocks());
it.each([{ accessToken: "legacy", refreshToken: "legacy" }, { memberId: 7, role: "ROOT" }])("세션 응답 %j가 현재 계약과 다르면 거부할 것이다", async data => {
  // given
  vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json({ isSuccess: true, timestamp, data }));
  // when
  const request = authApi.getSession();
  // then
  await expect(request).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
});
