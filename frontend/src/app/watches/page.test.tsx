import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { memberApi } from "@/lib/api";
import WatchesPage from "./page";

const authentication = vi.hoisted(
  (): { principal: { readonly memberId: number; readonly role: "USER" } | null } => ({
    principal: { memberId: 7, role: "USER" },
  }),
);

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    principal: authentication.principal,
    member: null,
    refreshMember: async (): Promise<void> => undefined,
    logout: (): void => undefined,
  }),
}));

describe("WatchesPage unavailable profile", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    authentication.principal = { memberId: 7, role: "USER" };
  });

  it("Given an authenticated principal without a profile, when watches load, then shows backend unavailability instead of a fabricated empty list", async () => {
    // Given
    const getMemberWatches = vi.spyOn(memberApi, "getMemberWatches");

    // When
    render(<WatchesPage />);

    // Then
    expect(await screen.findByText("백엔드 기능이 아직 제공되지 않습니다.")).toBeVisible();
    expect(getMemberWatches).toHaveBeenCalledWith(7);
    expect(screen.queryByText("아직 등록된 와치리스트가 없어요")).not.toBeInTheDocument();
  });

  it("Given the first watch request is pending, when the page first renders, then it never shows the empty watchlist", async () => {
    // Given
    const getMemberWatches = vi
      .spyOn(memberApi, "getMemberWatches")
      .mockImplementation(() => new Promise(() => undefined));
    let falseEmptyObserved = false;
    const observer = new MutationObserver(() => {
      falseEmptyObserved ||= document.body.textContent?.includes("아직 등록된 와치리스트가 없어요") ?? false;
    });
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });

    // When
    render(<WatchesPage />);

    // Then
    expect(screen.getByRole("progressbar")).toBeVisible();
    await Promise.resolve();
    observer.disconnect();
    expect(falseEmptyObserved).toBe(false);
    expect(screen.queryByText("아직 등록된 와치리스트가 없어요")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(getMemberWatches).toHaveBeenCalledWith(7);
  });
});
