import { render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/errors";
import { consumeOAuthTransaction, createOAuthTransaction } from "@/lib/oauthTransaction";
import OAuthCallbackPage from "./page";

const mocks = vi.hoisted(() => ({
  acceptSession: vi.fn(),
  exchange: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mocks.replace }),
}));

vi.mock("@/lib/api", () => ({
  authApi: { loginWithNaverCallback: mocks.exchange },
}));

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({ acceptSession: mocks.acceptSession }),
}));

function visit(query: string): void {
  history.replaceState(null, "", `/login/callback${query}`);
}

function prepareTransaction(returnPath = "/watches"): string {
  vi.spyOn(crypto, "randomUUID").mockReturnValue("fixture-state");
  return createOAuthTransaction(returnPath);
}

describe("OAuthCallbackPage", () => {
  beforeEach(() => {
    sessionStorage.clear();
    mocks.acceptSession.mockReset();
    mocks.exchange.mockReset();
    mocks.replace.mockReset();
    visit("");
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it("Given a matching transaction and valid tokens, when mounted in StrictMode, then cleans the query, exchanges once, accepts, and returns", async () => {
    // Given
    prepareTransaction("/watches?tab=mine");
    visit("?code=fixture-code&state=fixture-state");
    mocks.exchange.mockResolvedValue({
      success: true,
      data: {
        accessToken: "access.jwt.fixture",
        refreshToken: "refresh.jwt.fixture",
        accessTokenExpiry: 2_000_000_000,
      },
      timestamp: "2026-07-29T10:00:00",
    });
    mocks.acceptSession.mockReturnValue(true);

    // When
    render(
      <React.StrictMode>
        <OAuthCallbackPage />
      </React.StrictMode>,
    );

    // Then
    expect(location.pathname).toBe("/login/callback");
    expect(location.search).toBe("");
    await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/watches?tab=mine"));
    expect(mocks.exchange).toHaveBeenCalledOnce();
    expect(mocks.exchange).toHaveBeenCalledWith("fixture-code", "fixture-state");
    expect(mocks.acceptSession).toHaveBeenCalledOnce();
    expect(consumeOAuthTransaction("fixture-state")).toBeNull();
  });

  it("Given a provider rejection with untrusted details, when mounted, then renders an inert failure without exchange", async () => {
    // Given
    prepareTransaction();
    visit("?error=access_denied&error_description=%3Cscript%3Eprovider-secret%3C%2Fscript%3E");

    // When
    render(<OAuthCallbackPage />);

    // Then
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(screen.getByText("네이버 로그인이 취소되었거나 거부되었습니다.")).toBeVisible();
    expect(screen.queryByText(/provider-secret/u)).not.toBeInTheDocument();
    expect(location.search).toBe("");
    expect(mocks.exchange).not.toHaveBeenCalled();
  });

  it.each([
    ["missing code", "?state=fixture-state"],
    ["missing state", "?code=fixture-code"],
    ["mismatched state", "?code=fixture-code&state=wrong-state"],
    ["replayed state", "?code=fixture-code&state=fixture-state"],
  ])(
    "Given %s, when mounted, then rejects before exchange",
    async (scenario, query) => {
      // Given
      if (scenario !== "replayed state") prepareTransaction();
      if (scenario === "replayed state") {
        prepareTransaction();
        consumeOAuthTransaction("fixture-state");
      }
      visit(query);

      // When
      render(<OAuthCallbackPage />);

      // Then
      expect(await screen.findByRole("alert")).toBeVisible();
      expect(screen.getByText("로그인 요청이 유효하지 않거나 만료되었습니다.")).toBeVisible();
      expect(mocks.exchange).not.toHaveBeenCalled();
      expect(location.search).toBe("");
    },
  );

  it.each([
    ["2xx backend error", new ApiError("provider-secret", 200, "OAUTH_FAILED")],
    ["network failure", new ApiError("network-secret", 0, "NETWORK_ERROR")],
  ])(
    "Given a %s, when exchanged, then shows a fixed retry state without navigating",
    async (_scenario, failure) => {
      // Given
      prepareTransaction();
      visit("?code=fixture-code&state=fixture-state");
      mocks.exchange.mockRejectedValue(failure);

      // When
      render(<OAuthCallbackPage />);

      // Then
      expect(await screen.findByRole("alert")).toBeVisible();
      expect(screen.getByText("로그인을 완료할 수 없습니다. 다시 시도해주세요.")).toBeVisible();
      expect(screen.queryByText(/secret/u)).not.toBeInTheDocument();
      expect(mocks.replace).not.toHaveBeenCalled();
      expect(mocks.exchange).toHaveBeenCalledOnce();
    },
  );

  it("Given malformed JWT tokens, when session acceptance fails, then leaves the user in a retry state", async () => {
    // Given
    prepareTransaction();
    visit("?code=fixture-code&state=fixture-state");
    mocks.exchange.mockResolvedValue({
      success: true,
      data: {
        accessToken: "malformed-access",
        refreshToken: "malformed-refresh",
        accessTokenExpiry: 2_000_000_000,
      },
      timestamp: "2026-07-29T10:00:00",
    });
    mocks.acceptSession.mockReturnValue(false);

    // When
    render(<OAuthCallbackPage />);

    // Then
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(mocks.acceptSession).toHaveBeenCalledOnce();
    expect(mocks.replace).not.toHaveBeenCalled();
    expect(mocks.exchange).toHaveBeenCalledOnce();
  });

  it("Given a tampered unsafe return target, when mounted, then deletes the transaction and rejects before exchange", async () => {
    // Given
    sessionStorage.setItem(
      "watchtower_oauth_transaction",
      JSON.stringify({
        nonce: "fixture-state",
        returnPath: "https://attacker.example",
        createdAt: Date.now(),
      }),
    );
    visit("?code=fixture-code&state=fixture-state");

    // When
    render(<OAuthCallbackPage />);

    // Then
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(mocks.exchange).not.toHaveBeenCalled();
    expect(sessionStorage).toHaveLength(0);
  });

  it("Given an exchange interrupted by a real unmount, when the clean callback remounts, then no duplicate exchange occurs", async () => {
    // Given
    prepareTransaction();
    visit("?code=fixture-code&state=fixture-state");
    let resolveExchange = (): void => undefined;
    mocks.exchange.mockReturnValue(
      new Promise((resolve) => {
        resolveExchange = () =>
          resolve({
            success: true,
            data: {
              accessToken: "access.jwt.fixture",
              refreshToken: "refresh.jwt.fixture",
              accessTokenExpiry: 2_000_000_000,
            },
            timestamp: "2026-07-29T10:00:00",
          });
      }),
    );
    mocks.acceptSession.mockReturnValue(true);
    const first = render(<OAuthCallbackPage />);
    await waitFor(() => expect(mocks.exchange).toHaveBeenCalledOnce());

    // When
    first.unmount();
    render(<OAuthCallbackPage />);
    resolveExchange();

    // Then
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(mocks.exchange).toHaveBeenCalledOnce();
    expect(mocks.acceptSession).not.toHaveBeenCalled();
  });
});
