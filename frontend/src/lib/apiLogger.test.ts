import { afterEach, describe, expect, it, vi } from "vitest";
import { logError, logRequest, logResponse } from "./apiLogger";

describe("apiLogger", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given a browser request, when logged, then emits its request details in one console group", () => {
    const group = vi.spyOn(console, "groupCollapsed").mockImplementation(() => undefined);
    const log = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const groupEnd = vi.spyOn(console, "groupEnd").mockImplementation(() => undefined);

    logRequest({
      method: "GET",
      url: "/api/v1/watches",
      headers: { Accept: "application/json" },
      body: { page: 1 },
    });

    expect(group).toHaveBeenCalledOnce();
    expect(log.mock.calls).toEqual([
      ["Request URL:", "/api/v1/watches"],
      ["Method:", "GET"],
      ["Headers:", { Accept: "application/json" }],
      ["Body:", { page: 1 }],
    ]);
    expect(groupEnd).toHaveBeenCalledOnce();
  });

  it("Given nested request secrets, when logged, then recursively redacts them without mutating the input", () => {
    const authorizationSecret = "Bearer fixture-jwt-secret";
    const cookieSecret = "session=fixture-cookie-secret";
    const accessSecret = "fixture-access-secret";
    const refreshSecret = "fixture-refresh-secret";
    const body = {
      profile: { displayName: "Watch User" },
      tokens: [{ access_token: accessSecret }, { RefreshToken: refreshSecret }],
    };
    const original = structuredClone(body);
    const log = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const group = vi.spyOn(console, "groupCollapsed").mockImplementation(() => undefined);
    vi.spyOn(console, "groupEnd").mockImplementation(() => undefined);

    logRequest({
      method: "POST",
      url: "/api/v1/watches?code=fixture-code-secret&state=fixture-state-secret",
      headers: {
        Authorization: authorizationSecret,
        Cookie: cookieSecret,
        "X-Request-ID": "request-123",
      },
      body,
    });

    const rendered = JSON.stringify([group.mock.calls, log.mock.calls]);
    const containsSecret = [
      authorizationSecret,
      cookieSecret,
      accessSecret,
      refreshSecret,
      "fixture-code-secret",
      "fixture-state-secret",
    ].some((secret) => rendered.includes(secret));
    expect(containsSecret).toBe(false);
    expect(rendered.includes("Watch User")).toBe(true);
    expect(rendered.includes("request-123")).toBe(true);
    expect(body).toEqual(original);
  });

  it("Given an OAuth callback response, when logged, then redacts its body at the console boundary", () => {
    const log = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const group = vi.spyOn(console, "groupCollapsed").mockImplementation(() => undefined);
    vi.spyOn(console, "groupEnd").mockImplementation(() => undefined);

    logResponse({
      method: "POST",
      url: "/api/v1/auth/naver/callback?code=fixture-code-secret&state=fixture-state-secret",
      status: 200,
      statusText: "OK",
      durationMs: 12,
      body: { opaqueCredential: "fixture-callback-body-secret" },
    });

    const rendered = JSON.stringify([group.mock.calls, log.mock.calls]);
    const containsSecret = [
      "fixture-code-secret",
      "fixture-state-secret",
      "fixture-callback-body-secret",
    ].some((secret) => rendered.includes(secret));
    expect(containsSecret).toBe(false);
    expect(rendered.includes("[REDACTED]")).toBe(true);
  });

  it("Given a normal API response containing an authorize URL, when logged, then redacts nonce and token query values", () => {
    const nonce = "fixture-authorize-state-secret";
    const code = "fixture-authorize-code-secret";
    const accessToken = "fixture-authorize-access-token";
    const refreshToken = "fixture-authorize-refresh-token";
    const log = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const group = vi.spyOn(console, "groupCollapsed").mockImplementation(() => undefined);
    vi.spyOn(console, "groupEnd").mockImplementation(() => undefined);

    logResponse({
      method: "GET",
      url: "/api/v1/auth/naver/url?state=outer-state-secret",
      status: 200,
      statusText: "OK",
      durationMs: 12,
      body: `https://nid.naver.com/oauth2.0/authorize?client_id=watchtower-client&state=${nonce}&code=${code}&access_token=${accessToken}&refreshToken=${refreshToken}`,
    });

    const rendered = JSON.stringify([group.mock.calls, log.mock.calls]);
    const containsSecret = [nonce, code, accessToken, refreshToken, "outer-state-secret"].some((secret) =>
      rendered.includes(secret),
    );
    expect(containsSecret).toBe(false);
    expect(rendered.includes("watchtower-client")).toBe(true);
    expect(rendered.includes("REDACTED")).toBe(true);
  });

  it("Given an authorize URL with fragment credentials, when logged, then keeps every console argument secret-free", () => {
    const accessToken = "fixture-fragment-access-token";
    const refreshToken = "fixture-fragment-refresh-token";
    const state = "fixture-fragment-state";
    const code = "fixture-fragment-code";
    const cookie = "fixture-fragment-cookie";
    const log = vi.spyOn(console, "log").mockImplementation(() => undefined);
    const group = vi.spyOn(console, "groupCollapsed").mockImplementation(() => undefined);
    vi.spyOn(console, "groupEnd").mockImplementation(() => undefined);

    logResponse({
      method: "GET",
      url: "/api/v1/auth/naver/url",
      status: 200,
      statusText: "OK",
      durationMs: 12,
      body: `https://idp.example/callback#access_token=${accessToken}&refresh_token=${refreshToken}&state=${state}&code=${code}&cookie=${cookie}`,
    });

    const rendered = JSON.stringify([group.mock.calls, log.mock.calls]);
    const containsSecret = [accessToken, refreshToken, state, code, cookie].some((secret) =>
      rendered.includes(secret),
    );
    expect(containsSecret).toBe(false);
    expect(rendered.includes("REDACTED")).toBe(true);
  });

  it("Given an error message containing OAuth query and fragment credentials, when logged, then keeps every console argument secret-free", () => {
    const accessToken = "fixture-error-access-token";
    const refreshToken = "fixture-error-refresh-token";
    const state = "fixture-error-state";
    const code = "fixture-error-code";
    const error = vi.spyOn(console, "error").mockImplementation(() => undefined);
    const group = vi.spyOn(console, "groupCollapsed").mockImplementation(() => undefined);
    vi.spyOn(console, "groupEnd").mockImplementation(() => undefined);

    logError({
      method: "POST",
      url: "/api/v1/auth/naver/callback",
      durationMs: 12,
      error: new Error(
        `https://idp.example/callback?code=${code}&state=${state}#access_token=${accessToken}&refresh_token=${refreshToken}`,
      ),
    });

    const rendered = JSON.stringify([group.mock.calls, error.mock.calls]);
    const containsSecret = [accessToken, refreshToken, state, code].some((secret) => rendered.includes(secret));
    expect(containsSecret).toBe(false);
    expect(rendered.includes("REDACTED")).toBe(true);
  });

  it("Given a malformed URL containing query-like OAuth credentials, when logged, then redacts every console argument through the fallback", () => {
    const accessToken = "fixture-malformed-access-token";
    const refreshToken = "fixture-malformed-refresh-token";
    const state = "fixture-malformed-state";
    const code = "fixture-malformed-code";
    const error = vi.spyOn(console, "error").mockImplementation(() => undefined);
    const group = vi.spyOn(console, "groupCollapsed").mockImplementation(() => undefined);
    vi.spyOn(console, "groupEnd").mockImplementation(() => undefined);

    logError({
      method: "GET",
      url: `https://[invalid?code=${code}&state=${state}#access_token=${accessToken}&refresh_token=${refreshToken}`,
      durationMs: 12,
      error: new Error("request failed"),
    });

    const rendered = JSON.stringify([group.mock.calls, error.mock.calls]);
    const containsSecret = [accessToken, refreshToken, state, code].some((secret) => rendered.includes(secret));
    expect(containsSecret).toBe(false);
    expect(rendered.includes("REDACTED")).toBe(true);
  });
});
