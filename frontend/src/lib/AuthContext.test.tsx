import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BackendFeatureUnavailableError } from "@/lib/api/errors";
import { clearSession, commitSession, readSession } from "@/lib/session";
import { AuthProvider, useAuth } from "./AuthContext";

const NOW_SECONDS = 2_000_000_000;
const { getMember, logout } = vi.hoisted(() => ({
  getMember: vi.fn(),
  logout: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  authApi: { logout },
  memberApi: { getMember },
}));

function createJwt(payload: object): string {
  const encode = (value: object): string =>
    btoa(JSON.stringify(value))
      .replaceAll("+", "-")
      .replaceAll("/", "_")
      .replaceAll("=", "");

  return `${encode({ alg: "none", typ: "JWT" })}.${encode(payload)}.fixture`;
}

function installSession(memberId = "7", role: "USER" | "ADMIN" = "USER"): void {
  const [accessToken, refreshToken] = createTokens(memberId, role);
  commitSession(accessToken, refreshToken);
}

function createTokens(
  memberId = "7",
  role: "USER" | "ADMIN" = "USER",
): readonly [string, string] {
  return [
    createJwt({
      sub: memberId,
      role,
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 60,
    }),
    createJwt({
      sub: memberId,
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 600,
    }),
  ];
}

function AuthProbe(): React.JSX.Element {
  const { isLoggedIn, member, memberStatus, principal, logout } = useAuth();
  return (
    <>
      <output aria-label="login-state">{isLoggedIn ? "authenticated" : "anonymous"}</output>
      <output aria-label="principal">{principal?.memberId ?? "none"}</output>
      <output aria-label="profile-state">{member === null ? "none" : "present"}</output>
      <output aria-label="profile-status">{memberStatus}</output>
      <output aria-label="nickname">{member?.nickname ?? "profile-unavailable"}</output>
      <button onClick={logout}>로컬 로그아웃</button>
    </>
  );
}

function AcceptSessionProbe({
  accessToken,
  refreshToken,
  onNavigate,
}: {
  readonly accessToken: string;
  readonly refreshToken: string;
  readonly onNavigate: () => void;
}): React.JSX.Element {
  const { acceptSession, isLoggedIn } = useAuth();
  const handleAccept = (): void => {
    if (acceptSession({ accessToken, refreshToken })) onNavigate();
  };

  return (
    <>
      <output aria-label="login-state">{isLoggedIn ? "authenticated" : "anonymous"}</output>
      <button onClick={handleAccept}>세션 승인</button>
    </>
  );
}

describe("AuthProvider existing member behavior", () => {
  beforeEach(() => {
    vi.setSystemTime(NOW_SECONDS * 1_000);
    clearSession();
    getMember.mockReset();
    logout.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    clearSession();
  });

  it("Given a valid session and member response, when mounted, then renders the authenticated member", async () => {
    // Given
    installSession();
    getMember.mockResolvedValue({
      success: true,
      data: {
        id: 7,
        nickname: "감시자",
        email: "watcher@example.com",
        profileImageUrl: "",
        role: "USER",
        lastLoginAt: "2033-05-18T00:00:00Z",
      },
      timestamp: "2033-05-18T00:00:00Z",
    });

    // When
    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
    );

    // Then
    expect(await screen.findByLabelText("nickname")).toHaveTextContent("감시자");
    expect(screen.getByLabelText("login-state")).toHaveTextContent("authenticated");
  });

  it("Given a valid session with unavailable profile, when mounted in StrictMode, then restores the principal immediately", async () => {
    // Given
    installSession();
    getMember.mockRejectedValue(new BackendFeatureUnavailableError("member.profile"));

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <AuthProbe />
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByLabelText("login-state")).toHaveTextContent("authenticated");
    expect(screen.getByLabelText("principal")).toHaveTextContent("7");
    await waitFor(() => {
      expect(screen.getByLabelText("profile-state")).toHaveTextContent("none");
      expect(screen.getByLabelText("profile-status")).toHaveTextContent("unavailable");
    });
  });

  it("Given a structurally valid profile, when restoration resolves, then enriches the optional member", async () => {
    // Given
    installSession();
    getMember.mockResolvedValue({
      success: true,
      data: {
        id: 7,
        nickname: "실제 회원",
        email: "member@example.com",
        profileImageUrl: "",
        role: "USER",
        lastLoginAt: "2033-05-18T00:00:00Z",
      },
      timestamp: "2033-05-18T00:00:00Z",
    });

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <AuthProbe />
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(await screen.findByLabelText("nickname")).toHaveTextContent("실제 회원");
    expect(screen.getByLabelText("profile-state")).toHaveTextContent("present");
  });

  it("Given an empty-string placeholder profile, when restoration resolves, then keeps auth without fabricating a member", async () => {
    // Given
    installSession();
    getMember.mockResolvedValue({
      success: true,
      data: "",
      timestamp: "2033-05-18T00:00:00Z",
    });

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <AuthProbe />
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByLabelText("login-state")).toHaveTextContent("authenticated");
    await waitFor(() => {
      expect(screen.getByLabelText("profile-state")).toHaveTextContent("none");
    });
  });

  it("Given valid callback tokens, when accepted, then updates auth and commits before navigation", () => {
    // Given
    const [accessToken, refreshToken] = createTokens("19", "ADMIN");
    const onNavigate = vi.fn(() => readSession());
    getMember.mockRejectedValue(new BackendFeatureUnavailableError("member.profile"));
    render(
      <React.StrictMode>
        <AuthProvider>
          <AcceptSessionProbe
            accessToken={accessToken}
            refreshToken={refreshToken}
            onNavigate={onNavigate}
          />
        </AuthProvider>
      </React.StrictMode>,
    );

    // When
    fireEvent.click(screen.getByRole("button", { name: "세션 승인" }));

    // Then
    expect(onNavigate).toHaveReturnedWith({ memberId: 19, role: "ADMIN" });
    expect(screen.getByLabelText("login-state")).toHaveTextContent("authenticated");
  });

  it("Given a restored session, when locally logged out, then clears both cookies and all auth state", async () => {
    // Given
    installSession();
    getMember.mockRejectedValue(new BackendFeatureUnavailableError("member.profile"));
    render(
      <React.StrictMode>
        <AuthProvider>
          <AuthProbe />
        </AuthProvider>
      </React.StrictMode>,
    );

    // When
    fireEvent.click(screen.getByRole("button", { name: "로컬 로그아웃" }));

    // Then
    await waitFor(() => {
      expect(screen.getByLabelText("login-state")).toHaveTextContent("anonymous");
    });
    expect(document.cookie).not.toContain("watchtower_jwt=");
    expect(document.cookie).not.toContain("watchtower_refresh=");
    expect(logout).not.toHaveBeenCalled();
  });

  it("Given malformed JWT cookies, when mounted, then stays anonymous and clears stale state", () => {
    // Given
    document.cookie = "watchtower_jwt=malformed; Path=/";
    document.cookie = "watchtower_refresh=malformed; Path=/";

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <AuthProbe />
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByLabelText("login-state")).toHaveTextContent("anonymous");
    expect(document.cookie).not.toContain("watchtower_jwt=");
    expect(document.cookie).not.toContain("watchtower_refresh=");
  });

});
