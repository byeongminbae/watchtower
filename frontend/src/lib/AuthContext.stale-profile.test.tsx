import { act, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearSession, commitSession } from "@/lib/session";
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

function installSession(): void {
  commitSession(
    createJwt({
      sub: "7",
      role: "NORMAL",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 60,
    }),
    createJwt({
      sub: "7",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 600,
    }),
  );
}

function StaleProfileProbe(): React.JSX.Element {
  const { isLoggedIn, member, logout } = useAuth();
  return (
    <>
      <output aria-label="login-state">{isLoggedIn ? "authenticated" : "anonymous"}</output>
      <output aria-label="profile-state">{member === null ? "none" : "present"}</output>
      <button onClick={logout}>로컬 로그아웃</button>
    </>
  );
}

describe("AuthProvider stale profile handling", () => {
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

  it("Given a pending profile request, when logged out before it resolves, then stale profile cannot restore state", async () => {
    // Given
    installSession();
    let resolveProfile: (value: object) => void = () => undefined;
    getMember.mockReturnValue(
      new Promise<object>((resolve) => {
        resolveProfile = resolve;
      }),
    );
    render(
      <React.StrictMode>
        <AuthProvider>
          <StaleProfileProbe />
        </AuthProvider>
      </React.StrictMode>,
    );

    // When
    fireEvent.click(screen.getByRole("button", { name: "로컬 로그아웃" }));
    await act(async () => {
      resolveProfile({
        success: true,
        data: {
          id: 7,
          nickname: "늦은 회원",
          email: "late@example.com",
          profileImageUrl: "",
          role: "NORMAL",
          lastSignInAt: "2033-05-18T00:00:00Z",
        },
        timestamp: "2033-05-18T00:00:00Z",
      });
    });

    // Then
    expect(screen.getByLabelText("login-state")).toHaveTextContent("anonymous");
    expect(screen.getByLabelText("profile-state")).toHaveTextContent("none");
  });
});
