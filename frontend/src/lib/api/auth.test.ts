import { afterEach, describe, expect, it, vi } from "vitest";
import { authApi } from "./auth";

const TIMESTAMP = "2026-07-29T10:00:00";

function response(data: unknown): Response {
  return new Response(
    JSON.stringify({ success: true, data, timestamp: TIMESTAMP }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}

describe("authApi.getNaverLoginUrl", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given a Naver HTTPS authorize URL, when requested, then returns the parsed provider URL", async () => {
    // Given
    const authorizeUrl =
      "https://nid.naver.com/oauth2.0/authorize?response_type=code&state=fixture-state";
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response(authorizeUrl));

    // When
    const result = await authApi.getNaverLoginUrl("fixture-state");

    // Then
    expect(result.data).toBe(authorizeUrl);
  });

  it("Given an attacker-controlled URL, when requested, then rejects the malformed response", async () => {
    // Given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response("https://attacker.example/oauth"));

    // When
    const request = authApi.getNaverLoginUrl("fixture-state");

    // Then
    await expect(request).rejects.toMatchObject({
      name: "ApiError",
      status: 200,
      code: "INVALID_RESPONSE",
    });
  });

  it.each([
    "not a URL",
    "http://nid.naver.com/oauth2.0/authorize",
    "https://nid.naver.com/oauth2.0/token",
    "https://nid.naver.com.evil.example/oauth2.0/authorize",
  ])("Given malformed provider URL %s, when requested, then rejects it", async (url) => {
    // Given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response(url));

    // When
    const request = authApi.getNaverLoginUrl("fixture-state");

    // Then
    await expect(request).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
  });
});

describe("authApi.loginWithNaverCallback", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given the backend token contract, when the callback is exchanged, then returns the typed token pair", async () => {
    // Given
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      response({
        accessToken: "access.jwt.fixture",
        refreshToken: "refresh.jwt.fixture",
        accessTokenExpiry: 2_000_000_000,
      }),
    );

    // When
    const result = await authApi.loginWithNaverCallback("fixture-code", "fixture-state");

    // Then
    expect(result.data).toEqual({
      accessToken: "access.jwt.fixture",
      refreshToken: "refresh.jwt.fixture",
      accessTokenExpiry: 2_000_000_000,
    });
    expect(fetch).toHaveBeenCalledWith(
      "/api/v1/auth/naver/callback?code=fixture-code&state=fixture-state",
      expect.objectContaining({ method: "GET" }),
    );
  });

  it.each([
    { accessToken: "", refreshToken: "refresh", accessTokenExpiry: 2_000_000_000 },
    { accessToken: "access", refreshToken: "", accessTokenExpiry: 2_000_000_000 },
    { accessToken: "access", refreshToken: "refresh" },
    { accessToken: "access", refreshToken: "refresh", accessTokenExpiry: "soon" },
  ])(
    "Given malformed callback data %#, when exchanged, then rejects the response",
    async (data) => {
      // Given
      vi.spyOn(globalThis, "fetch").mockResolvedValue(response(data));

      // When
      const request = authApi.loginWithNaverCallback("fixture-code", "fixture-state");

      // Then
      await expect(request).rejects.toMatchObject({
        name: "ApiError",
        code: "INVALID_RESPONSE",
      });
    },
  );
});
