import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { memberApi } from "@/lib/api";
import SubscriptionPage from "./page";

const authentication = vi.hoisted(() => ({
  principal: { memberId: 7, role: "USER" as const },
}));

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    principal: authentication.principal,
    member: null,
  }),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe("SubscriptionPage unavailable profile", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given an authenticated principal without a profile, when subscription loads, then shows backend unavailability instead of generic missing subscription content", async () => {
    // Given
    const getMemberSubscription = vi.spyOn(memberApi, "getMemberSubscription");

    // When
    render(<SubscriptionPage />);

    // Then
    expect(await screen.findByText("백엔드 기능이 아직 제공되지 않습니다.")).toBeVisible();
    expect(getMemberSubscription).toHaveBeenCalledWith(7);
    expect(screen.queryByText("구독 정보를 불러올 수 없습니다.")).not.toBeInTheDocument();
  });

  it("Given the first subscription request is pending, when the page renders, then it keeps unavailable and plan content hidden", async () => {
    // Given
    vi.spyOn(memberApi, "getMemberSubscription").mockImplementation(
      () => new Promise(() => undefined),
    );
    let priorContentObserved = false;
    const observer = new MutationObserver(() => {
      priorContentObserved ||= document.body.textContent?.includes("구독 정보를 불러올 수 없습니다.") ?? false;
    });
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });

    // When
    render(<SubscriptionPage />);

    // Then
    expect(screen.getByRole("progressbar")).toBeVisible();
    await Promise.resolve();
    observer.disconnect();
    expect(priorContentObserved).toBe(false);
    expect(screen.queryByText("구독 정보를 불러올 수 없습니다.")).not.toBeInTheDocument();
    expect(screen.queryByText("Free")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
