import { afterEach, describe, expect, it, vi } from "vitest";
import { adminApi } from "./admin";
import { authApi } from "./auth";
import {
  backendFeatureInventory,
  rejectUnavailable,
} from "./availability";
import { memberApi } from "./member";
import { paymentApi } from "./payment";
import { watchApi } from "./watch";

const TIMESTAMP = "2026-07-29T10:00:00";
type HasOnlyFeatureParameter =
  Parameters<typeof rejectUnavailable> extends readonly [unknown] ? true : false;
const HAS_ONLY_FEATURE_PARAMETER: HasOnlyFeatureParameter = true;

describe("backend API availability inventory", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given the unavailable helper type, when inspected, then only a feature ID is representable", () => {
    // Given / When / Then
    expect(HAS_ONLY_FEATURE_PARAMETER).toBe(true);
  });

  it("Given the implemented Naver URL contract, when requested, then preserves its current fetch success", async () => {
    // Given
    const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          data: "https://nid.naver.com/oauth2.0/authorize",
          timestamp: TIMESTAMP,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    // When
    const result = await authApi.getNaverLoginUrl("fixture-state");

    // Then
    expect(result.data).toBe("https://nid.naver.com/oauth2.0/authorize");
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });

  it("Given the implemented Naver callback contract, when exchanged, then performs one guarded callback fetch", async () => {
    // Given
    const tokens = {
      accessToken: "fixture-access",
      refreshToken: "fixture-refresh",
      accessTokenExpiry: 2_000_000_000,
    };
    const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          data: tokens,
          timestamp: TIMESTAMP,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    // When
    const result = await authApi.loginWithNaverCallback(
      "fixture code",
      "fixture state",
    );

    // Then
    expect(result.data).toEqual(tokens);
    expect(fetchSpy).toHaveBeenCalledTimes(1);
    expect(fetchSpy).toHaveBeenCalledWith(
      "/bff/auth/naver/callback?code=fixture%20code&state=fixture%20state",
      expect.objectContaining({ method: "GET" }),
    );
  });

  it("Given the implemented member profile contract, when requested, then performs one guarded member fetch", async () => {
    // Given
    const member = {
      id: 7,
      nickname: "감시자",
      email: "watcher@example.com",
      profileImageUrl: "https://example.com/profile.png",
      lastSignInAt: "2026-07-29T10:00:00",
    };
    const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          success: true,
          data: member,
          timestamp: TIMESTAMP,
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    // When
    const result = await memberApi.getMember(7);

    // Then
    expect(result.data).toEqual(member);
    expect(fetchSpy).toHaveBeenCalledWith(
      "/bff/member/7",
      expect.objectContaining({ method: "GET" }),
    );
  });

  const unavailableCases = [
    { feature: "auth.revokeNaverToken", invoke: () => authApi.revokeNaverToken() },
    { feature: "member.updateMember", invoke: () => memberApi.updateMember(1, { nickname: "수정" }) },
    { feature: "member.deleteMember", invoke: () => memberApi.deleteMember(1) },
    { feature: "member.getMemberWatches", invoke: () => memberApi.getMemberWatches(1) },
    {
      feature: "member.getMemberPaymentHistories",
      invoke: () => memberApi.getMemberPaymentHistories(1),
    },
    { feature: "member.getMemberSubscription", invoke: () => memberApi.getMemberSubscription(1) },
    {
      feature: "member.cancelMemberSubscription",
      invoke: () => memberApi.cancelMemberSubscription(1),
    },
    {
      feature: "watch.createWatch",
      invoke: () =>
        watchApi.createWatch({
          name: "와치",
          url: "https://example.test",
          intervalSeconds: 60,
          includeInStat: false,
          conditions: [{ type: "KEYWORD", keyword: "변경" }],
        }),
    },
    { feature: "watch.getTrendingWatches", invoke: () => watchApi.getTrendingWatches() },
    { feature: "watch.getWatch", invoke: () => watchApi.getWatch(1) },
    { feature: "watch.updateWatch", invoke: () => watchApi.updateWatch(1, { status: "PAUSED" }) },
    { feature: "watch.deleteWatch", invoke: () => watchApi.deleteWatch(1) },
    { feature: "watch.sendTestNotification", invoke: () => watchApi.sendTestNotification(1) },
    { feature: "watch.getWatchConditions", invoke: () => watchApi.getWatchConditions(1) },
    {
      feature: "watch.createWatchCondition",
      invoke: () => watchApi.createWatchCondition(1, { type: "KEYWORD", keyword: "변경" }),
    },
    {
      feature: "watch.updateWatchCondition",
      invoke: () => watchApi.updateWatchCondition(1, 2, { keyword: "수정" }),
    },
    {
      feature: "watch.deleteWatchCondition",
      invoke: () => watchApi.deleteWatchCondition(1, 2),
    },
    { feature: "watch.listWatchSnapshots", invoke: () => watchApi.listWatchSnapshots(1) },
    { feature: "watch.getWatchSnapshot", invoke: () => watchApi.getWatchSnapshot(1, 2) },
    { feature: "payment.getPayment", invoke: () => paymentApi.getPayment(1) },
    {
      feature: "payment.confirmTossPayment",
      invoke: () =>
        paymentApi.confirmTossPayment({
          paymentKey: "fixture-key",
          orderId: "fixture-order",
          amount: 1000,
        }),
    },
    { feature: "admin.getWatches", invoke: () => adminApi.getWatches() },
    {
      feature: "admin.updateWatchStatus",
      invoke: () => adminApi.updateWatchStatus(1, "PAUSED"),
    },
    { feature: "admin.cancelPayment", invoke: () => adminApi.cancelPayment(1) },
    { feature: "admin.getPayments", invoke: () => adminApi.getPayments() },
    { feature: "admin.getUsers", invoke: () => adminApi.getUsers() },
    { feature: "admin.updateUserRole", invoke: () => adminApi.updateUserRole(1, "ADMIN") },
    { feature: "admin.updateUserStatus", invoke: () => adminApi.updateUserStatus(1, true) },
    { feature: "admin.getWatchStats", invoke: () => adminApi.getWatchStats() },
    { feature: "admin.getUserStats", invoke: () => adminApi.getUserStats() },
    { feature: "admin.getPaymentStats", invoke: () => adminApi.getPaymentStats() },
    { feature: "admin.getAuthStats", invoke: () => adminApi.getAuthStats() },
  ] as const;

  it("Given the backend contract inventory, when counted, then five features are callable", () => {
    // Given
    const callableFeatures = Object.entries(backendFeatureInventory)
      .filter(([, entry]) => entry.contractClass === "callable")
      .map(([feature]) => feature);

    // When
    const callableFeatureCount = callableFeatures.length;

    // Then
    expect(callableFeatureCount).toBe(5);
    expect(callableFeatures).toEqual([
      "auth.getNaverLoginUrl",
      "auth.loginWithNaverCallback",
      "auth.renewSession",
      "auth.logout",
      "member.getMember",
    ]);
  });

  it.each(unavailableCases)(
    "Given $feature is non-callable, when invoked, then rejects before fetch with its stable feature ID",
    async ({ feature, invoke }) => {
      // Given
      const fetchSpy = vi.spyOn(globalThis, "fetch");

      // When
      const request = invoke();

      // Then
      await expect(request).rejects.toEqual(
        expect.objectContaining({
          name: "BackendFeatureUnavailableError",
          feature,
        }),
      );
      expect(fetchSpy).not.toHaveBeenCalled();
    },
  );

  it("Given every current API object export, when compared with the invocation table, then none is unclassified", () => {
    // Given
    const invokedFeatures = unavailableCases.map(({ feature }) => feature);
    const currentExportFeatures = [
      ...Object.keys(authApi).map((method) => `auth.${method}`),
      ...Object.keys(memberApi).map((method) => `member.${method}`),
      ...Object.keys(watchApi).map((method) => `watch.${method}`),
      ...Object.keys(paymentApi).map((method) => `payment.${method}`),
      ...Object.keys(adminApi).map((method) => `admin.${method}`),
    ].filter(
      (feature) =>
        feature !== "auth.getNaverLoginUrl" &&
        feature !== "auth.loginWithNaverCallback" &&
        feature !== "auth.renewSession" &&
        feature !== "auth.logout" &&
        feature !== "member.getMember",
    );

    // When
    const classifiedFeatures = Object.keys(backendFeatureInventory);

    // Then
    expect(invokedFeatures).toEqual(currentExportFeatures);
    expect(classifiedFeatures).toEqual(
      expect.arrayContaining([
        "auth.getNaverLoginUrl",
        "auth.loginWithNaverCallback",
        "member.getMember",
        ...currentExportFeatures,
      ]),
    );
  });
});
