import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { consumeOAuthTransaction } from "@/lib/oauthTransaction";
import LoginPage from "./page";

const mocks = vi.hoisted(() => ({
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

describe("LoginPage Discord login initiation", () => {
  beforeEach(() => {
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
    expect(screen.queryByRole("button", { name: "Discord로 시작하기" })).not.toBeInTheDocument();
  });

  it("Given no explicit return target, when Discord login starts, then stores the root page for restoration", async () => {
    // Given
    mocks.search = "";
    const navigate = vi.fn();
    vi.stubGlobal("location", { assign: navigate });
    render(<LoginPage />);

    // When
    fireEvent.click(await screen.findByRole("button", { name: "Discord로 시작하기" }));

    // Then
    await waitFor(() => expect(navigate).toHaveBeenCalledOnce());
    expect(consumeOAuthTransaction()).toBe("/");
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("Given a return target, when the login button is clicked repeatedly, then stores the target and moves to the Discord login route once", async () => {
    // Given
    const navigate = vi.fn();
    vi.stubGlobal("location", { assign: navigate });
    render(<LoginPage />);
    const button = await screen.findByRole("button", { name: "Discord로 시작하기" });

    // When
    fireEvent.click(button);
    fireEvent.click(button);

    // Then
    expect(navigate).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith("/bff/oauth2/authorization/discord");
    expect(consumeOAuthTransaction()).toBe("/watches?tab=mine");
    expect(screen.getByRole("button", { name: "Discord로 연결하고 있어요" })).toBeDisabled();
  });
});
