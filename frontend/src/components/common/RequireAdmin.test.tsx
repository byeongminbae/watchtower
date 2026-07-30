import { render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "@/lib/AuthContext";
import { clearSession, commitSession } from "@/lib/session";
import RequireAdmin from "./RequireAdmin";

const NOW_SECONDS = 2_000_000_000;
const { getMember, replace } = vi.hoisted(() => ({
  getMember: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  memberApi: { getMember },
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin",
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

function installSession(role: "NORMAL" | "ADMIN"): void {
  commitSession(
    createJwt({
      sub: "11",
      role,
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 60,
    }),
    createJwt({
      sub: "11",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 600,
    }),
  );
}

describe("RequireAdmin", () => {
  beforeEach(() => {
    vi.setSystemTime(NOW_SECONDS * 1_000);
    clearSession();
    getMember.mockReset();
    replace.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    clearSession();
  });

  it("Given an ADMIN principal and mismatched NORMAL profile, when mounted in StrictMode, then renders admin children", () => {
    // Given
    installSession("ADMIN");
    getMember.mockResolvedValue({
      success: true,
      data: {
        id: 11,
        nickname: "관리자",
        email: "admin@example.com",
        profileImageUrl: "",
        role: "NORMAL",
        lastSignInAt: "2033-05-18T00:00:00Z",
      },
      timestamp: "2033-05-18T00:00:00Z",
    });

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <RequireAdmin>
            <span>관리 화면</span>
          </RequireAdmin>
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByText("관리 화면")).toBeVisible();
    expect(replace).not.toHaveBeenCalled();
  });

  it("Given a NORMAL principal, when mounted, then renders the access-denied state", () => {
    // Given
    installSession("NORMAL");
    getMember.mockReturnValue(new Promise(() => undefined));

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <RequireAdmin>
            <span>관리 화면</span>
          </RequireAdmin>
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByText("접근 권한이 없습니다")).toBeVisible();
    expect(screen.queryByText("관리 화면")).not.toBeInTheDocument();
  });
});
