import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/errors";
import { consumeOAuthTransaction, createOAuthTransaction } from "@/lib/oauthTransaction";
import OAuthCallbackPage from "./page";

const mocks = vi.hoisted(() => ({
  acceptSession: vi.fn(),
  getSession: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

vi.mock("@/lib/api", () => ({
  authApi: { getSession: mocks.getSession },
}));

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({ acceptSession: mocks.acceptSession }),
}));

function visit(query: string): void {
  history.replaceState(null, "", `/login/callback${query}`);
}

const validSession = {
  isSuccess: true,
  data: { memberId: 7, role: "NORMAL" },
  timestamp: "2026-07-29T10:00:00",
};

describe("OAuthCallbackPage", () => {
  beforeEach(() => {
    sessionStorage.clear();
    mocks.acceptSession.mockReset();
    mocks.getSession.mockReset();
    mocks.replace.mockReset();
    visit("");
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it("Given a stored return target and a valid session, when mounted in StrictMode, then loads the session once, accepts it, and returns", async () => {
    // Given
    createOAuthTransaction("/watches?tab=mine");
    mocks.getSession.mockResolvedValue(validSession);
    mocks.acceptSession.mockReturnValue(true);

    // When
    render(
      <React.StrictMode>
        <OAuthCallbackPage />
      </React.StrictMode>,
    );

    // Then
    await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/watches?tab=mine"));
    expect(mocks.getSession).toHaveBeenCalledOnce();
    expect(mocks.acceptSession).toHaveBeenCalledWith(validSession.data);
    expect(consumeOAuthTransaction()).toBeNull();
  });

  it("Given no stored return target, when the session is valid, then returns to the root page", async () => {
    // Given
    mocks.getSession.mockResolvedValue(validSession);
    mocks.acceptSession.mockReturnValue(true);

    // When
    render(<OAuthCallbackPage />);

    // Then
    await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/"));
  });

  it("Given a tampered unsafe return target, when the session is valid, then returns to the safe fallback page", async () => {
    // Given
    sessionStorage.setItem(
      "watchtower_oauth_transaction",
      JSON.stringify({ returnPath: "https://attacker.example", createdAt: Date.now() }),
    );
    mocks.getSession.mockResolvedValue(validSession);
    mocks.acceptSession.mockReturnValue(true);

    // When
    render(<OAuthCallbackPage />);

    // Then
    await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/watches"));
  });

  it.each([
    ["access_denied", "Discord 로그인을 취소했거나 거부했어요."],
    ["authorization_request_not_found", "로그인 요청이 유효하지 않거나 만료됐어요."],
    ["AUT_006", "탈퇴한 계정이에요."],
    ["AUT_007", "이용이 정지된 계정이에요."],
    ["IDP_002", "다른 회원에 연결된 Discord 계정이에요."],
    ["IDP_003", "이미 Discord 계정이 연결되어 있어요."],
    ["server_error", "로그인을 완료할 수 없어요. 다시 시도해 주세요."],
  ])(
    "Given login error %s, when mounted, then shows its message without loading the session",
    async (errorCode, message) => {
      // Given
      createOAuthTransaction("/watches");
      visit(`?error=${errorCode}&error_description=%3Cscript%3Eprovider-secret%3C%2Fscript%3E`);

      // When
      render(<OAuthCallbackPage />);

      // Then
      expect(await screen.findByRole("alert")).toBeVisible();
      expect(screen.getByText(message)).toBeVisible();
      expect(screen.queryByText(/provider-secret/u)).not.toBeInTheDocument();
      expect(location.search).toBe("");
      expect(mocks.getSession).not.toHaveBeenCalled();
      expect(sessionStorage).toHaveLength(0);
    },
  );

  it.each([
    ["unauthenticated session", new ApiError("session-secret", 401, "AUT_001")],
    ["network failure", new ApiError("network-secret", 0, "NETWORK_ERROR")],
  ])(
    "Given a %s, when the session is loaded, then shows a fixed retry state without navigating",
    async (_scenario, failure) => {
      // Given
      mocks.getSession.mockRejectedValue(failure);

      // When
      render(<OAuthCallbackPage />);

      // Then
      expect(await screen.findByRole("alert")).toBeVisible();
      expect(screen.getByText("로그인을 완료할 수 없어요. 다시 시도해 주세요.")).toBeVisible();
      expect(screen.queryByText(/secret/u)).not.toBeInTheDocument();
      expect(mocks.replace).not.toHaveBeenCalled();
    },
  );

  it("Given an invalid session, when session acceptance fails, then leaves the user in a retry state", async () => {
    // Given
    mocks.getSession.mockResolvedValue(validSession);
    mocks.acceptSession.mockReturnValue(false);

    // When
    render(<OAuthCallbackPage />);

    // Then
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(mocks.acceptSession).toHaveBeenCalledOnce();
    expect(mocks.replace).not.toHaveBeenCalled();
  });
});
