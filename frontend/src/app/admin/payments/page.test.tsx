import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import AdminPaymentsPage from "@/app/admin/payments/page";
import { adminApi } from "@/lib/api/admin";

const PAYMENT = {
  id: 7,
  planName: "BASIC",
  planPrice: 1000,
  planDurationDays: 30,
  planTier: "BASIC",
  createdAt: "2026-07-29T10:00:00",
} as const;

describe("AdminPaymentsPage unavailable mutation", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Given a visible payment, when cancellation is unavailable twice, then keeps the dialog and never refetches or renders success", async () => {
    // Given
    const getPaymentsSpy = vi.spyOn(adminApi, "getPayments").mockResolvedValue({
      success: true,
      data: [PAYMENT],
      timestamp: "2026-07-29T10:00:00",
    });
    const cancelPaymentSpy = vi.spyOn(adminApi, "cancelPayment");
    const fetchSpy = vi.spyOn(globalThis, "fetch");
    render(<AdminPaymentsPage />);
    fireEvent.click(await screen.findByRole("button", { name: "직권 취소" }));
    const dialog = await screen.findByRole("dialog");

    // When
    const confirmButton = within(dialog).getByRole("button", {
      name: "직권 취소",
    });
    fireEvent.click(confirmButton);
    await screen.findByText("백엔드 기능이 아직 제공되지 않습니다.");
    await waitFor(() => expect(confirmButton).toBeEnabled());
    fireEvent.click(confirmButton);

    // Then
    await waitFor(() => {
      expect(cancelPaymentSpy).toHaveBeenCalledTimes(2);
    });
    expect(within(dialog).getByText("결제를 직권 취소할까요?")).toBeVisible();
    expect(getPaymentsSpy).toHaveBeenCalledTimes(1);
    expect(fetchSpy).not.toHaveBeenCalled();
    expect(screen.queryByText(/취소했습니다/)).not.toBeInTheDocument();
  });
});
