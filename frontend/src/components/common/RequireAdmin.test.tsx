import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import Guard from "./RequireAdmin";
const mocks = vi.hoisted(() => ({ initializing: false, loggedIn: false, role: "NORMAL", replace: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: mocks.replace }) }));
vi.mock("@/lib/AuthContext", () => ({ useAuth: () => ({ isInitializing: mocks.initializing, isLoggedIn: mocks.loggedIn, principal: { memberId: 7, role: mocks.role } }) }));
beforeEach(() => { mocks.initializing = false; mocks.loggedIn = false; mocks.role = "NORMAL"; mocks.replace.mockClear(); });
it("세션 복원이 진행 중이면 보호된 내용을 표시하지 않을 것이다", () => {
  // given
  mocks.initializing = true;
  // when
  render(<Guard><div>보호된 내용</div></Guard>);
  // then
  expect(screen.getByRole("progressbar")).toBeVisible();
  expect(screen.queryByText("보호된 내용")).not.toBeInTheDocument();
  expect(mocks.replace).not.toHaveBeenCalled();
});
it("비로그인 상태이면 원래 경로를 포함한 로그인 화면으로 이동할 것이다", async () => {
  // given
  history.replaceState(null, "", "/admin?page=2");
  // when
  render(<Guard><div>보호된 내용</div></Guard>);
  // then
  await waitFor(() => expect(mocks.replace).toHaveBeenCalledWith("/login?next=%2Fadmin%3Fpage%3D2"));
  expect(screen.queryByText("보호된 내용")).not.toBeInTheDocument();
});
it("필요한 로그인과 관리자 권한이 있으면 보호된 내용을 표시할 것이다", () => {
  // given
  mocks.loggedIn = true; mocks.role = "ADMIN";
  // when
  render(<Guard><div>보호된 내용</div></Guard>);
  // then
  expect(screen.getByText("보호된 내용")).toBeVisible();
});
it("일반 회원이면 관리자 내용을 표시하지 않을 것이다", () => {
  // given
  mocks.loggedIn = true;
  // when
  render(<Guard><div>보호된 내용</div></Guard>);
  // then
  expect(screen.queryByText("보호된 내용")).not.toBeInTheDocument();
  expect(screen.getByText("접근 권한이 없습니다")).toBeVisible();
});
