import { expect, it, vi } from "vitest";
import PaymentsPage from "./page";
const redirect = vi.hoisted(() => vi.fn());
vi.mock("next/navigation", () => ({ redirect }));
it("삭제된 결제와 구독 화면에 접근하면 현재 관리 화면으로 이동할 것이다", () => {
  // given
  redirect.mockClear();
  // when
  PaymentsPage();
  // then
  expect(redirect).toHaveBeenCalledWith("/mypage");
});
