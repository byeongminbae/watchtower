import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import Header from "./Header";
const mocks = vi.hoisted(() => ({ admin: false, nickname: "회원", loggedIn: true, logout: vi.fn(), push: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: mocks.push }), usePathname: () => "/watches" }));
vi.mock("@/lib/AuthContext", () => ({ useAuth: () => ({ isLoggedIn: mocks.loggedIn,
  principal: { memberId: 7, role: mocks.admin ? "ADMIN" : "NORMAL" }, member: { nickname: mocks.nickname }, logout: mocks.logout }) }));
beforeEach(() => { vi.clearAllMocks(); mocks.admin = false; mocks.nickname = "회원"; mocks.loggedIn = true; mocks.logout.mockResolvedValue(undefined); });
it("일반 회원은 타워와 알림 채널 메뉴를 사용할 수 있을 것이다", async () => {
  // given
  render(<Header />);
  // when
  fireEvent.click(screen.getByRole("button", { name: "회원 메뉴" }));
  // then
  expect(await screen.findByRole("menuitem", { name: "알림 채널" })).toHaveAttribute("href", "/mypage/notifications");
  expect(screen.queryByRole("menuitem", { name: "결제 이력" })).not.toBeInTheDocument();
});
it("관리자는 일반 회원 기능과 관리자 메뉴를 함께 사용할 수 있을 것이다", () => {
  // given
  mocks.admin = true;
  // when
  render(<Header />);
  // then
  expect(screen.getByRole("link", { name: "관리자" })).toHaveAttribute("href", "/admin");
  expect(screen.getByRole("link", { name: "내 타워" })).toHaveAttribute("href", "/watches");
});
it("닉네임에 태그가 들어가도 실행 가능한 HTML로 만들지 않을 것이다", () => {
  // given
  mocks.nickname = "<script>fixture</script>";
  // when
  const { container } = render(<Header />);
  // then
  expect(screen.getByRole("button", { name: "<script>fixture</script> 메뉴" })).toBeVisible();
  expect(container.querySelector("script")).toBeNull();
});
it("로그아웃이 완료되면 홈으로 이동할 것이다", async () => {
  // given
  render(<Header />);
  fireEvent.click(screen.getByRole("button", { name: "회원 메뉴" }));
  // when
  fireEvent.click(await screen.findByRole("menuitem", { name: "로그아웃" }));
  // then
  await waitFor(() => expect(mocks.push).toHaveBeenCalledWith("/"));
});
