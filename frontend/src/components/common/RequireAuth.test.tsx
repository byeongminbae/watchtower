import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "@/lib/AuthContext";
import { clearSession, commitSession } from "@/lib/session";
import RequireAuth from "./RequireAuth";

const NOW_SECONDS = 2_000_000_000;
const { getMember, logout, replace } = vi.hoisted(() => ({
  getMember: vi.fn(),
  logout: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  authApi: { logout },
  memberApi: { getMember },
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/watches",
  useRouter: () => ({ replace }),
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

function LogoutButton(): React.JSX.Element {
  const { logout } = useAuth();
  return <button onClick={logout}>로그아웃 실행</button>;
}

describe("RequireAuth", () => {
  beforeEach(() => {
    vi.setSystemTime(NOW_SECONDS * 1_000);
    history.replaceState(null, "", "/watches");
    clearSession();
    getMember.mockReset();
    logout.mockReset();
    replace.mockReset();
  });

  it("Given an anonymous deep link, when the guard redirects to login, then preserves its query and fragment", async () => {
    // Given
    history.replaceState(null, "", "/watches?tab=mine#changes");

    // When
    render(
      <AuthProvider>
        <RequireAuth>
          <span>보호된 화면</span>
        </RequireAuth>
      </AuthProvider>,
    );

    // Then
    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith(
        "/login?next=%2Fwatches%3Ftab%3Dmine%23changes",
      );
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    clearSession();
  });

  it("Given a valid member session, when profile restoration completes, then renders protected children", async () => {
    // Given
    installSession();
    getMember.mockResolvedValue({
      success: true,
      data: {
        id: 7,
        nickname: "감시자",
        email: "watcher@example.com",
        profileImageUrl: "",
        role: "NORMAL",
        lastSignInAt: "2033-05-18T00:00:00Z",
      },
      timestamp: "2033-05-18T00:00:00Z",
    });

    // When
    render(
      <AuthProvider>
        <RequireAuth>
          <span>보호된 화면</span>
        </RequireAuth>
      </AuthProvider>,
    );

    // Then
    expect(await screen.findByText("보호된 화면")).toBeVisible();
    expect(replace).not.toHaveBeenCalled();
  });

  it("Given an authenticated child before profile response, when mounted, then renders without waiting", () => {
    // Given
    installSession();
    getMember.mockReturnValue(new Promise(() => undefined));

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <RequireAuth>
            <span>보호된 화면</span>
          </RequireAuth>
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByText("보호된 화면")).toBeVisible();
  });

  it("Given an authenticated guarded child, when locally logged out, then redirects to login", async () => {
    // Given
    installSession();
    getMember.mockReturnValue(new Promise(() => undefined));
    render(
      <React.StrictMode>
        <AuthProvider>
          <RequireAuth>
            <LogoutButton />
          </RequireAuth>
        </AuthProvider>
      </React.StrictMode>,
    );

    // When
    fireEvent.click(screen.getByRole("button", { name: "로그아웃 실행" }));

    // Then
    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/login?next=%2Fwatches");
    });
  });
});
