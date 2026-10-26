import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import MyPage from "./page";
const mocks = vi.hoisted(() => ({ update: vi.fn(), remove: vi.fn(), refresh: vi.fn(), replace: vi.fn() }));
vi.mock("@/lib/api", () => ({ memberApi: { updateMember: mocks.update, deleteMember: mocks.remove } }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: mocks.replace }) }));
vi.mock("@/lib/AuthContext", () => ({ useAuth: () => ({
  member: { id: 7, nickname: "감시자", email: "member@example.com", profileImageUrl: "https://example.com/provider-avatar.png", lastSignInAt: "2026-10-26T12:00:00" },
  memberStatus: "ready", refreshMember: mocks.refresh
}) }));
beforeEach(() => { vi.resetAllMocks(); mocks.update.mockResolvedValue({}); mocks.refresh.mockResolvedValue(undefined); mocks.remove.mockResolvedValue({}); vi.stubGlobal("location", { replace: mocks.replace }); });
afterEach(() => vi.unstubAllGlobals());
it("닉네임을 수정하면 제공받은 프로필 사진을 유지하고 다시 조회할 것이다", async () => {
  // given
  render(<MyPage />);
  fireEvent.click(screen.getByRole("button", { name: "프로필 수정" }));
  // when
  fireEvent.change(screen.getByLabelText("닉네임"), { target: { value: "새 감시자" } });
  fireEvent.click(screen.getByRole("button", { name: "저장" }));
  // then
  await screen.findByText("프로필을 저장했어요.");
  expect(mocks.update).toHaveBeenCalledWith(7, { nickname: "새 감시자", profileImageUrl: "https://example.com/provider-avatar.png" });
  expect(mocks.refresh).toHaveBeenCalledOnce();
  expect(screen.queryByLabelText("프로필 사진 URL")).not.toBeInTheDocument();
  expect(screen.queryByText("변화를 받을 곳")).not.toBeInTheDocument();
  expect(screen.queryByText("Watchtower와 함께 지켜보고 있어요.")).not.toBeInTheDocument();
});
it("회원 탈퇴를 확인하면 계정을 삭제하고 홈으로 이동할 것이다", async () => {
  // given
  render(<MyPage />);
  fireEvent.click(screen.getByRole("button", { name: "회원 탈퇴" }));
  // when
  fireEvent.click(screen.getByRole("button", { name: /^탈퇴$/ }));
  // then
  await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/"));
  expect(mocks.remove).toHaveBeenCalledWith(7);
});
it("프로필 저장이 실패하면 오류를 표시하고 수정 상태를 유지할 것이다", async () => {
  // given
  mocks.update.mockRejectedValue(new Error("저장 실패"));
  render(<MyPage />);
  fireEvent.click(screen.getByRole("button", { name: "프로필 수정" }));
  // when
  fireEvent.click(screen.getByRole("button", { name: "저장" }));
  // then
  await screen.findByText("저장 실패");
  expect(screen.getByLabelText("닉네임")).toBeEnabled();
  expect(mocks.refresh).not.toHaveBeenCalled();
});
