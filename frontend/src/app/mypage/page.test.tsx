import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { memberApi } from "@/lib/api";
import type { Member } from "@/types/domain";
import MyPage from "./page";

const authentication = vi.hoisted(
  (): {
    principal: { readonly memberId: number; readonly role: "USER" };
    member: Member | null;
  } => ({
    principal: { memberId: 7, role: "USER" },
    member: null,
  }),
);

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    principal: authentication.principal,
    member: authentication.member,
    refreshMember: async (): Promise<void> => undefined,
    logout: (): void => undefined,
  }),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe("MyPage unavailable profile", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    authentication.member = null;
  });

  it("Given an authenticated principal without a profile, when membership data loads, then shows backend unavailability instead of hiding the page", async () => {
    // Given
    const getMemberSubscription = vi.spyOn(memberApi, "getMemberSubscription");

    // When
    render(<MyPage />);

    // Then
    expect(await screen.findByText("백엔드 기능이 아직 제공되지 않습니다.")).toBeVisible();
    expect(getMemberSubscription).toHaveBeenCalledWith(7);
    expect(screen.queryByRole("heading", { name: "마이페이지" })).not.toBeInTheDocument();
  });

  it("Given the first subscription request is pending, when a real profile is available, then it keeps profile content hidden behind pending UI", async () => {
    // Given
    authentication.member = {
      id: 7,
      nickname: "감시자",
      email: "watcher@example.com",
      profileImageUrl: "",
      role: "USER",
      lastLoginAt: "2026-07-29T00:00:00Z",
    };
    vi.spyOn(memberApi, "getMemberSubscription").mockImplementation(
      () => new Promise(() => undefined),
    );
    let priorContentObserved = false;
    const observer = new MutationObserver(() => {
      priorContentObserved ||= document.body.textContent?.includes("마이페이지") ?? false;
    });
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });

    // When
    render(<MyPage />);

    // Then
    expect(screen.getByRole("progressbar")).toBeVisible();
    await Promise.resolve();
    observer.disconnect();
    expect(priorContentObserved).toBe(false);
    expect(screen.queryByRole("heading", { name: "마이페이지" })).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
