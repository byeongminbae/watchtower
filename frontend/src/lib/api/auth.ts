import { apiClient } from "./client";
import type { ApiEnvelope } from "./types";

export const authApi = {
  // GET /api/v1/auth/naver/url?state=...
  // state: 로그인 완료 후 백엔드가 최종적으로 302 리다이렉트할 프론트 경로.
  // 백엔드가 이 값을 JWT로 인코딩한 state에 담아 위변조를 방지한다.
  getNaverLoginUrl: (state: string) =>
    apiClient.get<ApiEnvelope<string>>(
      `/api/v1/auth/naver/url?state=${encodeURIComponent(state)}`,
    ),

  // DELETE /api/v1/auth/naver/revoke
  revokeNaverToken: () => apiClient.delete<ApiEnvelope<string>>("/api/v1/auth/naver/revoke"),

  // POST /api/v1/auth/renew
  // 응답 바디가 아니라 Set-Cookie로 JWT 쿠키를 갱신해준다고 가정한다.
  renewSession: () => apiClient.post<ApiEnvelope<string>>("/api/v1/auth/renew"),

  // DELETE /api/v1/auth/logout
  // 백엔드가 JWT 쿠키를 삭제(만료 처리)해준다고 가정한다.
  logout: () => apiClient.delete<ApiEnvelope<string>>("/api/v1/auth/logout"),
};
