import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { memberApi } from "@/lib/api";
import type { Member } from "@/types/domain";
import MyPage from "./page";

const authentication = vi.hoisted(
  (): {
    principal: { readonly memberId: number; readonly role: "NORMAL" };
    member: Member | null;
    memberStatus: "ready";
    renewSession: ReturnType<typeof vi.fn>;
  } => ({
    principal: { memberId: 7, role: "NORMAL" },
    member: null,
    memberStatus: "ready",
    renewSession: vi.fn(),
  }),
);

vi.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    principal: authentication.principal,
    member: authentication.member,
    memberStatus: authentication.memberStatus,
    refreshMember: async (): Promise<void> => undefined,
    renewSession: authentication.renewSession,
    logout: async (): Promise<void> => undefined,
  }),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe("MyPage member profile", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    authentication.member = null;
    authentication.renewSession.mockReset();
  });

  it("Given a loaded member profile, when MyPage opens, then shows current information without requesting subscription data", () => {
    // Given
    const getMemberSubscription = vi.spyOn(memberApi, "getMemberSubscription");
    authentication.member = {
      id: 7,
      nickname: "감시자",
      email: "watcher@example.com",
      profileImageUrl: "https://example.com/profile.png",
      role: "NORMAL",
      lastSignInAt: "2026-07-29T00:00:00Z",
    };

    // When
    render(<MyPage />);

    // Then
    expect(screen.getByRole("heading", { name: "마이페이지" })).toBeVisible();
    expect(screen.getAllByDisplayValue("감시자")).not.toHaveLength(0);
    expect(screen.getAllByDisplayValue("watcher@example.com")).not.toHaveLength(0);
    expect(
      screen.getByRole("img", { name: "감시자 프로필 사진" }),
    ).toHaveAttribute("src", "https://example.com/profile.png");
    expect(screen.getByRole("button", { name: "수정" })).toBeVisible();
    expect(getMemberSubscription).not.toHaveBeenCalled();
  });

  it("Given current member information, when the edit button is clicked, then enables nickname editing with save and cancel actions", () => {
    // Given
    authentication.member = {
      id: 7,
      nickname: "감시자",
      email: "watcher@example.com",
      profileImageUrl: "",
      role: "NORMAL",
      lastSignInAt: "2026-07-29T00:00:00Z",
    };
    render(<MyPage />);

    // When
    fireEvent.click(screen.getByRole("button", { name: "수정" }));

    // Then
    expect(screen.getByRole("textbox", { name: "닉네임" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "저장" })).toBeVisible();
    expect(screen.getByRole("button", { name: "취소" })).toBeVisible();
  });

  it("Given an active session, when session renewal is selected, then renews and shows success", async () => {
    // Given
    authentication.member = {
      id: 7,
      nickname: "감시자",
      email: "watcher@example.com",
      profileImageUrl: "",
      role: "NORMAL",
      lastSignInAt: "2026-07-29T00:00:00Z",
    };
    authentication.renewSession.mockResolvedValue(true);
    render(<MyPage />);

    // When
    fireEvent.click(screen.getByRole("button", { name: "세션 갱신" }));

    // Then
    expect(await screen.findByText("세션이 갱신되었습니다.")).toBeVisible();
    expect(authentication.renewSession).toHaveBeenCalledTimes(1);
  });
});
