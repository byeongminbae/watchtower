import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./errors";
import { apiClient } from "./client";
import type { ApiEnvelope } from "./types";

const TIMESTAMP = "2026-07-29T10:00:00";

type TokenPair = {
  readonly accessToken: string;
  readonly refreshToken: string;
};

function response(body: object, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function isTokenPair(value: unknown): value is TokenPair {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value) &&
    "accessToken" in value &&
    typeof value.accessToken === "string" &&
    "refreshToken" in value &&
    typeof value.refreshToken === "string"
  );
}

describe("apiClient response boundary", () => {
  beforeEach(() => {
    document.cookie = "watchtower_jwt=; Max-Age=0; Path=/";
    document.cookie = "watchtower_refresh=; Max-Age=0; Path=/";
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given a valid token-pair envelope, when fetched with its guard, then returns parsed tokens", async () => {
    // Given
    const envelope: ApiEnvelope<TokenPair> = {
      success: true,
      data: {
        accessToken: "fixture-access",
        refreshToken: "fixture-refresh",
      },
      timestamp: TIMESTAMP,
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response(envelope));

    // When
    const result = await apiClient.get<TokenPair>(
      "/api/v1/auth/naver/callback",
      isTokenPair,
    );

    // Then
    expect(result).toEqual(envelope);
  });

  it("Given a success envelope without data, when fetched, then returns the empty success envelope", async () => {
    // Given
    const envelope = { success: true, timestamp: TIMESTAMP };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response(envelope));

    // When
    const result = await apiClient.deleteEmpty("/api/v1/auth/logout");

    // Then
    expect(result).toEqual(envelope);
  });

  it.each([
    {
      scenario: "missing data",
      envelope: { success: true, timestamp: TIMESTAMP },
    },
    {
      scenario: "null data",
      envelope: { success: true, data: null, timestamp: TIMESTAMP },
    },
    {
      scenario: "wrong-shaped token data",
      envelope: {
        success: true,
        data: { accessToken: "fixture-access" },
        timestamp: TIMESTAMP,
      },
    },
  ])(
    "Given $scenario when token data is required, when fetched, then rejects the envelope",
    async ({ envelope }) => {
      // Given
      vi.spyOn(globalThis, "fetch").mockResolvedValue(response(envelope));

      // When
      const request = apiClient.get<TokenPair>(
        "/api/v1/auth/naver/callback",
        isTokenPair,
      );

      // Then
      await expect(request).rejects.toMatchObject({
        name: "ApiError",
        status: 200,
        code: "INVALID_RESPONSE",
      });
    },
  );

  it.each([
    { scenario: "HTTP error", httpStatus: 404 },
    { scenario: "2xx business error", httpStatus: 200 },
  ])(
    "Given an error envelope with $scenario, when fetched, then throws its typed backend error",
    async ({ httpStatus }) => {
      // Given
      const envelope = {
        success: false,
        statusCode: "MEMBER_NOT_FOUND",
        message: "회원을 찾을 수 없습니다.",
        timestamp: TIMESTAMP,
      };
      vi.spyOn(globalThis, "fetch").mockResolvedValue(response(envelope, httpStatus));

      // When
      const request = apiClient.get<ApiEnvelope<string>>("/api/v1/member/404");

      // Then
      await expect(request).rejects.toMatchObject({
        name: "ApiError",
        status: httpStatus,
        code: "MEMBER_NOT_FOUND",
        message: envelope.message,
        raw: envelope,
      });
    },
  );

  it("Given malformed JSON, when fetched, then throws a deterministic malformed-response error", async () => {
    // Given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response("{broken", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );

    // When
    const request = apiClient.get<ApiEnvelope<string>>("/api/v1/member/1");

    // Then
    await expect(request).rejects.toMatchObject({
      name: "ApiError",
      status: 200,
      code: "MALFORMED_RESPONSE",
    });
  });

  it("Given a malformed business envelope, when fetched, then treats external fields as invalid data", async () => {
    // Given
    const malformedEnvelope = {
      success: false,
      statusCode: 401,
      message: "<script>fixture</script>",
      timestamp: TIMESTAMP,
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response(malformedEnvelope));

    // When
    const request = apiClient.get<ApiEnvelope<string>>("/api/v1/member/1");

    // Then
    await expect(request).rejects.toMatchObject({
      name: "ApiError",
      status: 200,
      code: "INVALID_RESPONSE",
      raw: malformedEnvelope,
    });
  });

  it("Given an empty required response body, when fetched, then throws a deterministic empty-response error", async () => {
    // Given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response("", { status: 200 }));

    // When
    const request = apiClient.get<ApiEnvelope<string>>("/api/v1/member/1");

    // Then
    await expect(request).rejects.toMatchObject({
      name: "ApiError",
      status: 200,
      code: "EMPTY_RESPONSE",
    });
  });

  it("Given fetch rejects, when requested, then throws a typed network error", async () => {
    // Given
    const cause = new TypeError("fixture connection refused");
    const fetchSpy = vi.spyOn(globalThis, "fetch").mockRejectedValue(cause);

    // When
    const request = apiClient.get<ApiEnvelope<string>>("/api/v1/member/1");

    // Then
    await expect(request).rejects.toEqual(
      expect.objectContaining({
        name: "ApiError",
        status: 0,
        code: "NETWORK_ERROR",
        raw: cause,
      }),
    );
    expect(fetchSpy).toHaveBeenCalledTimes(1);
  });

  it("Given a stale local session and 401, when requested, then clears session and never renews or retries", async () => {
    // Given
    document.cookie = "watchtower_jwt=stale-access; Path=/";
    document.cookie = "watchtower_refresh=stale-refresh; Path=/";
    const requestedUrls: string[] = [];
    vi.spyOn(globalThis, "fetch").mockImplementation(async (input, init) => {
      const requestFixture = new Request(
        new URL(String(input), "https://watchtower.test"),
        init,
      );
      requestedUrls.push(requestFixture.url);
      return response(
        {
          success: false,
          statusCode: "UNAUTHORIZED",
          message: "인증이 만료되었습니다.",
          timestamp: TIMESTAMP,
        },
        401,
      );
    });

    // When
    const request = apiClient.get<ApiEnvelope<string>>("/api/v1/member/1");

    // Then
    await expect(request).rejects.toBeInstanceOf(ApiError);
    expect(document.cookie).not.toContain("watchtower_jwt=");
    expect(document.cookie).not.toContain("watchtower_refresh=");
    expect(requestedUrls).toEqual(["https://watchtower.test/api/v1/member/1"]);
    expect(requestedUrls.some((url) => url.includes("/auth/renew"))).toBe(false);
  });
});
