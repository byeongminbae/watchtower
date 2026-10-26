import React from "react";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";
import { clearSession } from "./session";
import { ApiError } from "./api/errors";

const mocks = vi.hoisted(() => ({ session: vi.fn(), member: vi.fn(), logout: vi.fn() }));
vi.mock("@/lib/api", () => ({ authApi: { getSession: mocks.session, logout: mocks.logout }, memberApi: { getMember: mocks.member } }));
const profile = { id: 7, nickname: "회원", email: "test@example.com", profileImageUrl: "", lastSignInAt: "2026-10-26T10:00:00" };
function Probe() {
  const auth = useAuth();
  const [error, setError] = React.useState("");
  return <>
    <output aria-label="회원">{auth.member?.nickname ?? "없음"}</output>
    <output aria-label="권한">{auth.principal?.role ?? "없음"}</output>
    <output aria-label="초기화">{String(auth.isInitializing)}</output>
    <output aria-label="오류">{auth.sessionError ?? error}</output>
    <button onClick={() => void auth.logout().catch((failure: unknown) => { if (failure instanceof Error) setError(failure.message); })}>로그아웃</button>
  </>;
}
beforeEach(() => {
  clearSession(); vi.resetAllMocks();
  mocks.session.mockResolvedValue({ data: { memberId: 7, role: "NORMAL" } });
  mocks.member.mockResolvedValue({ data: profile });
  mocks.logout.mockResolvedValue({ isSuccess: true });
});
it("새로고침할 때 서버 세션을 복원하면 해당 회원 프로필을 조회할 것이다", async () => {
  // given
  render(<React.StrictMode><AuthProvider><Probe /></AuthProvider></React.StrictMode>);
  // when
  await screen.findByText("회원");
  // then
  expect(screen.getByLabelText("권한")).toHaveTextContent("NORMAL");
  expect(screen.getByLabelText("초기화")).toHaveTextContent("false");
  expect(mocks.member).toHaveBeenCalledWith(7);
});
it("서버 로그아웃이 완료되면 로그인과 프로필 상태를 제거할 것이다", async () => {
  // given
  render(<AuthProvider><Probe /></AuthProvider>);
  await screen.findByText("회원");
  // when
  fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));
  // then
  await waitFor(() => expect(screen.getByLabelText("권한")).toHaveTextContent("없음"));
  expect(screen.getByLabelText("회원")).toHaveTextContent("없음");
});
it("로그아웃 요청이 실패하면 서버 세션이 종료된 것처럼 표시하지 않을 것이다", async () => {
  // given
  mocks.logout.mockRejectedValue(new ApiError("연결 실패", 0));
  render(<AuthProvider><Probe /></AuthProvider>);
  await screen.findByText("회원");
  // when
  fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));
  // then
  await screen.findByText("연결 실패");
  expect(screen.getByLabelText("권한")).toHaveTextContent("NORMAL");
});
it("뒤로 가기로 복원할 때 서버 권한이 바뀌었으면 최신 권한을 반영할 것이다", async () => {
  // given
  render(<AuthProvider><Probe /></AuthProvider>);
  await screen.findByText("회원");
  mocks.session.mockResolvedValue({ data: { memberId: 7, role: "ADMIN" } });
  // when
  await act(async () => window.dispatchEvent(new Event("pageshow")));
  // then
  await waitFor(() => expect(screen.getByLabelText("권한")).toHaveTextContent("ADMIN"));
});
it("세션 서버가 응답하지 않으면 초기화를 끝내고 연결 오류를 남길 것이다", async () => {
  // given
  mocks.session.mockRejectedValue(new ApiError("서버 연결 실패", 502));
  // when
  render(<AuthProvider><Probe /></AuthProvider>);
  // then
  await screen.findByText("서버 연결 실패");
  expect(screen.getByLabelText("초기화")).toHaveTextContent("false");
  expect(mocks.member).not.toHaveBeenCalled();
});
