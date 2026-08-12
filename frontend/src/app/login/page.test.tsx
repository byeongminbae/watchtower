import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { consumeOAuthTransaction } from "@/lib/oauthTransaction";
import LoginPage from "./page";

const mocks = vi.hoisted(() => ({
  getNaverLoginUrl: vi.fn(),
  isInitializing: false,
  isLoggedIn: false,
  locationReplace: vi.fn(),
  search: "next=%2Fwatches%3Ftab%3Dmine",
}));

vi.mock("next/navigation", () => ({
  useSearchParams: () => new URLSearchParams(mocks.search),
}));

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    isInitializing: mocks.isInitializing,
    isLoggedIn: mocks.isLoggedIn,
  }),
}));

vi.mock("@/lib/api", () => ({
  authApi: { getNaverLoginUrl: mocks.getNaverLoginUrl },
}));

describe("LoginPage Naver login initiation", () => {
  beforeEach(() => {
    mocks.getNaverLoginUrl.mockReset();
    mocks.isInitializing = false;
    mocks.isLoggedIn = false;
    mocks.locationReplace.mockReset();
    mocks.search = "next=%2Fwatches%3Ftab%3Dmine";
    sessionStorage.clear();
  });

  it("Given a valid authenticated session, when the login page mounts, then replaces it with the root page", async () => {
    // Given
    mocks.isLoggedIn = true;
    vi.stubGlobal("location", { replace: mocks.locationReplace });

    // When
    render(<LoginPage />);

    // Then
    await waitFor(() => expect(mocks.locationReplace).toHaveBeenCalledWith("/"));
    expect(screen.queryByRole("button", { name: "네이버로 시작하기" })).not.toBeInTheDocument();
  });

  it("Given no explicit return target, when Naver login starts, then stores the root page for restoration", async () => {
    // Given
    mocks.search = "";
    vi.spyOn(crypto, "randomUUID").mockReturnValue("fixture-nonce");
    mocks.getNaverLoginUrl.mockResolvedValue({
      success: true,
      data: "https://nid.naver.com/oauth2.0/authorize?state=fixture-nonce",
      timestamp: "2026-07-29T10:00:00",
    });
    const navigate = vi.fn();
    vi.stubGlobal("location", { assign: navigate });
    render(<LoginPage />);

    // When
    fireEvent.click(await screen.findByRole("button", { name: "네이버로 시작하기" }));

    // Then
    await waitFor(() => expect(navigate).toHaveBeenCalledOnce());
    expect(consumeOAuthTransaction("fixture-nonce")).toBe("/");
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("Given a valid provider URL, when the login button is clicked repeatedly, then creates one nonce, requests it once, and navigates once", async () => {
    // Given
    const nonce = vi.spyOn(crypto, "randomUUID").mockReturnValue("fixture-nonce");
    mocks.getNaverLoginUrl.mockResolvedValue({
      success: true,
      data: "https://nid.naver.com/oauth2.0/authorize?state=fixture-nonce",
      timestamp: "2026-07-29T10:00:00",
    });
    const navigate = vi.fn();
    vi.stubGlobal("location", { assign: navigate });
    render(<LoginPage />);
    const button = await screen.findByRole("button", { name: "네이버로 시작하기" });

    // When
    fireEvent.click(button);
    fireEvent.click(button);

    // Then
    await waitFor(() => expect(navigate).toHaveBeenCalledOnce());
    expect(mocks.getNaverLoginUrl).toHaveBeenCalledWith("fixture-nonce");
    expect(nonce).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith(
      "https://nid.naver.com/oauth2.0/authorize?state=fixture-nonce",
    );
  });

  it("Given an invalid provider URL, when the login button is clicked, then clears the transaction, shows an alert, and reenables the button", async () => {
    // Given
    mocks.search = "next=https%3A%2F%2Fattacker.example";
    vi.spyOn(crypto, "randomUUID").mockReturnValue("fixture-nonce");
    mocks.getNaverLoginUrl.mockRejectedValue(new Error("invalid provider URL"));
    const navigate = vi.fn();
    vi.stubGlobal("location", { assign: navigate });
    render(<LoginPage />);

    // When
    fireEvent.click(await screen.findByRole("button", { name: "네이버로 시작하기" }));

    // Then
    expect(await screen.findByRole("alert")).toBeVisible();
    expect(consumeOAuthTransaction("fixture-nonce")).toBeNull();
    expect(navigate).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "네이버로 시작하기" })).toBeEnabled();
  });
});
