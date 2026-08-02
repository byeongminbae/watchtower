import { act, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { hydrateRoot, type Root } from "react-dom/client";
import { renderToString } from "react-dom/server";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import RequireAuth from "@/components/common/RequireAuth";
import Header from "@/components/layout/Header";
import { AuthProvider, useAuth } from "@/lib/AuthContext";
import { clearSession, commitSession } from "@/lib/session";

const NOW_SECONDS = 2_000_000_000;
const { getMember, push, replace } = vi.hoisted(() => ({
  getMember: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  memberApi: { getMember },
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/watches",
  useRouter: () => ({ push, replace }),
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

function SessionProbe(): React.JSX.Element {
  const { isLoggedIn, member, memberStatus } = useAuth();
  return (
    <>
      <output aria-label="login-state">
        {isLoggedIn ? "authenticated" : "anonymous"}
      </output>
      <output aria-label="member-state">
        {member === null ? "none" : "present"}
      </output>
      <output aria-label="member-status">{memberStatus}</output>
    </>
  );
}

describe("AuthProvider unauthorized session handling", () => {
  beforeEach(() => {
    vi.setSystemTime(NOW_SECONDS * 1_000);
    history.replaceState(null, "", "/watches");
    clearSession();
    getMember.mockReset();
    push.mockReset();
    replace.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    clearSession();
  });

  it("Given a mounted authenticated shell, when the transport clears its session, then logs out without reload", async () => {
    // Given
    installSession();
    getMember.mockReturnValue(new Promise(() => undefined));
    render(
      <React.StrictMode>
        <AuthProvider>
          <Header />
          <RequireAuth>
            <span>보호된 화면</span>
          </RequireAuth>
        </AuthProvider>
      </React.StrictMode>,
    );
    expect(screen.getByRole("button", { name: "회원 메뉴" })).toBeVisible();
    expect(screen.getByText("보호된 화면")).toBeVisible();

    // When
    act(() => clearSession());

    // Then
    expect(document.cookie).not.toContain("watchtower_jwt=");
    expect(document.cookie).not.toContain("watchtower_refresh=");
    expect(await screen.findByRole("link", { name: "로그인" })).toBeVisible();
    expect(screen.queryByText("보호된 화면")).not.toBeInTheDocument();
    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/login?next=%2Fwatches");
    });
  });

  it("Given a pending profile request, when the session is cleared repeatedly before resolution, then stale data cannot restore auth state", async () => {
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
          <SessionProbe />
        </AuthProvider>
      </React.StrictMode>,
    );
    expect(screen.getByLabelText("login-state")).toHaveTextContent("authenticated");

    // When
    act(() => {
      clearSession();
      clearSession();
    });
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
    expect(screen.getByLabelText("member-state")).toHaveTextContent("none");
    expect(screen.getByLabelText("member-status")).toHaveTextContent("idle");
  });

  it("Given cookies unavailable during server rendering, when the authenticated client hydrates, then restores without a mismatch", async () => {
    // Given
    installSession();
    getMember.mockReturnValue(new Promise(() => undefined));
    vi.stubGlobal("document", undefined);
    const serverHtml = renderToString(
      <AuthProvider>
        <SessionProbe />
      </AuthProvider>,
    );
    vi.unstubAllGlobals();
    const container = document.createElement("div");
    container.innerHTML = serverHtml;
    document.body.append(container);
    const onRecoverableError = vi.fn();
    let root: Root | null = null;

    // When
    await act(async () => {
      root = hydrateRoot(
        container,
        <AuthProvider>
          <SessionProbe />
        </AuthProvider>,
        { onRecoverableError },
      );
    });

    // Then
    expect(serverHtml).toContain("anonymous");
    await waitFor(() => {
      expect(screen.getByLabelText("login-state")).toHaveTextContent("authenticated");
    });
    expect(onRecoverableError).not.toHaveBeenCalled();
    await act(async () => root?.unmount());
  });
});
