import { apiClient } from "./client";
import { isSessionPrincipal } from "@/lib/session";

export function isNaverAuthorizeUrl(value: unknown): value is string {
  if (typeof value !== "string") return false;
  try {
    const url = new URL(value);
    return url.protocol === "https:" && url.hostname === "nid.naver.com" &&
      url.port === "" && url.username === "" && url.password === "" &&
      url.pathname === "/oauth2.0/authorize";
  } catch (error) {
    if (error instanceof TypeError) return false;
    throw error;
  }
}

export const authApi = {
  getNaverLoginUrl: (state: string) =>
    apiClient.get(`/bff/auth/naver/url?state=${encodeURIComponent(state)}`, isNaverAuthorizeUrl),
  loginWithNaverCallback: (code: string, state: string) =>
    apiClient.get(`/bff/auth/naver/callback?code=${encodeURIComponent(code)}&state=${encodeURIComponent(state)}`, isSessionPrincipal),
  getSession: () => apiClient.get("/bff/auth/session", isSessionPrincipal),
  logout: () => apiClient.deleteEmpty("/bff/auth/logout")
};
