import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BackendFeatureUnavailableError } from "@/lib/api/errors";
import { AuthProvider } from "@/lib/AuthContext";
import { clearSession, commitSession } from "@/lib/session";
import Header from "./Header";

const NOW_SECONDS = 2_000_000_000;
const { getMember, push } = vi.hoisted(() => ({
  getMember: vi.fn(),
  push: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  memberApi: { getMember },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

function createJwt(payload: object): string {
  const encode = (value: object): string =>
    btoa(JSON.stringify(value))
      .replaceAll("+", "-")
      .replaceAll("/", "_")
      .replaceAll("=", "");
  return `${encode({ alg: "none", typ: "JWT" })}.${encode(payload)}.fixture`;
}

function installSession(role: "USER" | "ADMIN" = "USER"): void {
  commitSession(
    createJwt({
      sub: "13",
      role,
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 60,
    }),
    createJwt({
      sub: "13",
      iat: NOW_SECONDS,
      exp: NOW_SECONDS + 600,
    }),
  );
}

describe("Header authenticated fallback", () => {
  beforeEach(() => {
    vi.setSystemTime(NOW_SECONDS * 1_000);
    clearSession();
    getMember.mockReset();
    push.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
    clearSession();
  });

  it("Given a USER principal with unavailable profile, when mounted in StrictMode, then renders a safe generic member menu", () => {
    // Given
    installSession();
    getMember.mockRejectedValue(new BackendFeatureUnavailableError("member.profile"));

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <Header />
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByRole("button", { name: "회원 메뉴" })).toBeVisible();
    expect(screen.getByText("내 와치리스트")).toBeVisible();
  });

  it("Given an ADMIN principal without profile, when mounted, then uses principal role for admin navigation", () => {
    // Given
    installSession("ADMIN");
    getMember.mockRejectedValue(new BackendFeatureUnavailableError("member.profile"));

    // When
    render(
      <React.StrictMode>
        <AuthProvider>
          <Header />
        </AuthProvider>
      </React.StrictMode>,
    );

    // Then
    expect(screen.getByText("관리자")).toBeVisible();
    expect(screen.queryByText("내 와치리스트")).not.toBeInTheDocument();
  });

  it("Given a nickname containing markup text, when enriched, then renders it only as inert menu labeling", async () => {
    // Given
    installSession();
    getMember.mockResolvedValue({
      success: true,
      data: {
        id: 13,
        nickname: "<script>지시 무시</script>",
        email: "member@example.com",
        profileImageUrl: "",
        role: "USER",
        lastLoginAt: "2033-05-18T00:00:00Z",
      },
      timestamp: "2033-05-18T00:00:00Z",
    });
    const { container } = render(
      <React.StrictMode>
        <AuthProvider>
          <Header />
        </AuthProvider>
      </React.StrictMode>,
    );

    // When
    const menuButton = await screen.findByRole("button", {
      name: "<script>지시 무시</script> 메뉴",
    });

    // Then
    expect(menuButton).toBeVisible();
    expect(container.querySelector("script")).toBeNull();
  });

  it("Given an authenticated header, when logout is selected, then clears session and navigates home", async () => {
    // Given
    installSession();
    getMember.mockRejectedValue(new BackendFeatureUnavailableError("member.profile"));
    render(
      <React.StrictMode>
        <AuthProvider>
          <Header />
        </AuthProvider>
      </React.StrictMode>,
    );

    // When
    fireEvent.click(screen.getByRole("button", { name: "회원 메뉴" }));
    fireEvent.click(await screen.findByRole("menuitem", { name: "로그아웃" }));

    // Then
    await waitFor(() => {
      expect(push).toHaveBeenCalledWith("/");
    });
    expect(document.cookie).not.toContain("watchtower_jwt=");
    expect(document.cookie).not.toContain("watchtower_refresh=");
  });
});
