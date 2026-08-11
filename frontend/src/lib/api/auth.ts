import { apiClient } from "./client";
import { rejectUnavailable } from "./availability";
import type { ApiEnvelope } from "./types";

export type NaverCallbackTokens = {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly accessTokenExpiry: number;
};

function isNaverCallbackTokens(value: unknown): value is NaverCallbackTokens {
  return (
    typeof value === "object" &&
    value !== null &&
    !Array.isArray(value) &&
    "accessToken" in value &&
    typeof value.accessToken === "string" &&
    value.accessToken.length > 0 &&
    "refreshToken" in value &&
    typeof value.refreshToken === "string" &&
    value.refreshToken.length > 0 &&
    "accessTokenExpiry" in value &&
    typeof value.accessTokenExpiry === "number" &&
    Number.isSafeInteger(value.accessTokenExpiry) &&
    value.accessTokenExpiry > 0
  );
}

export function isNaverAuthorizeUrl(value: unknown): value is string {
  if (typeof value !== "string") return false;

  try {
    const url = new URL(value);
    return (
      url.protocol === "https:" &&
      url.hostname === "nid.naver.com" &&
      url.port === "" &&
      url.username === "" &&
      url.password === "" &&
      url.pathname === "/oauth2.0/authorize"
    );
  } catch (error) {
    if (error instanceof TypeError) return false;
    throw error;
  }
}

export const authApi = {
  getNaverLoginUrl: (state: string) =>
    apiClient.get(
      `/bff/auth/naver/url?state=${encodeURIComponent(state)}`,
      isNaverAuthorizeUrl,
    ),

  loginWithNaverCallback: (code: string, state: string) =>
    apiClient.get(
      `/bff/auth/naver/callback?code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`,
      isNaverCallbackTokens,
    ),

  revokeNaverToken: () =>
    rejectUnavailable<ApiEnvelope<string>>("auth.revokeNaverToken"),

  renewSession: (refreshToken: string) =>
    apiClient.post(
      "/bff/auth/renew",
      { refreshToken },
      isNaverCallbackTokens,
    ),

  logout: () => apiClient.deleteEmpty("/bff/auth/logout"),
};
