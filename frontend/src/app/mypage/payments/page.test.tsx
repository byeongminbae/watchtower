import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { memberApi } from "@/lib/api";
import PaymentsPage from "./page";

const authentication = vi.hoisted(() => ({
  principal: { memberId: 7, role: "NORMAL" as const },
}));

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    principal: authentication.principal,
    member: null,
  }),
}));

describe("PaymentsPage unavailable profile", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given an authenticated principal without a profile, when payment history loads, then shows backend unavailability instead of a fabricated empty history", async () => {
    // Given
    const getMemberPaymentHistories = vi.spyOn(memberApi, "getMemberPaymentHistories");

    // When
    render(<PaymentsPage />);

    // Then
    expect(await screen.findByText("백엔드 기능이 아직 제공되지 않습니다.")).toBeVisible();
    expect(getMemberPaymentHistories).toHaveBeenCalledWith(7);
    expect(screen.queryByText("결제 이력이 없습니다.")).not.toBeInTheDocument();
  });

  it("Given the first payment request is pending, when the page renders, then it keeps the empty history and table content hidden", async () => {
    // Given
    vi.spyOn(memberApi, "getMemberPaymentHistories").mockImplementation(
      () => new Promise(() => undefined),
    );
    let priorContentObserved = false;
    const observer = new MutationObserver(() => {
      priorContentObserved ||= document.body.textContent?.includes("결제 이력이 없습니다.") ?? false;
    });
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });

    // When
    render(<PaymentsPage />);

    // Then
    expect(screen.getByRole("progressbar")).toBeVisible();
    await Promise.resolve();
    observer.disconnect();
    expect(priorContentObserved).toBe(false);
    expect(screen.queryByText("결제 이력이 없습니다.")).not.toBeInTheDocument();
    expect(screen.queryByText("결제일")).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
