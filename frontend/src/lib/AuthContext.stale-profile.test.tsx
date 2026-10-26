import React from "react";
import { act, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./AuthContext";
import { clearSession, commitSession } from "./session";

const mocks = vi.hoisted(() => ({ session: vi.fn(), profile: vi.fn(), logout: vi.fn() }));
vi.mock("@/lib/api", () => ({ authApi: { getSession: mocks.session, logout: mocks.logout }, memberApi: { getMember: mocks.profile } }));
function Probe() { const auth = useAuth(); return <output aria-label="프로필">{auth.member?.nickname ?? "없음"}</output>; }
beforeEach(() => { clearSession(); vi.resetAllMocks(); mocks.session.mockResolvedValue({ data: { memberId: 7, role: "NORMAL" } }); });
it("로그아웃 후 이전 프로필 요청이 완료돼도 프로필을 복원하지 않을 것이다", async () => {
  // given
  let resolve: (value: object) => void = () => undefined;
  mocks.profile.mockReturnValue(new Promise<object>(done => { resolve = done; }));
  render(<AuthProvider><Probe /></AuthProvider>);
  await waitFor(() => expect(mocks.profile).toHaveBeenCalledWith(7));
  // when
  await act(async () => {
    clearSession();
    resolve({ data: { id: 7, nickname: "이전 회원", email: "old@example.com", profileImageUrl: "", lastSignInAt: "2026-10-26T10:00:00" } });
  });
  // then
  expect(screen.getByLabelText("프로필")).toHaveTextContent("없음");
});
it("다른 회원으로 전환하면 이전 회원의 지연된 응답을 표시하지 않을 것이다", async () => {
  // given
  let resolve: (value: object) => void = () => undefined;
  mocks.profile.mockImplementation((id: number) => id === 7 ? new Promise<object>(done => { resolve = done; }) :
    Promise.resolve({ data: { id: 8, nickname: "현재 회원", email: "new@example.com", profileImageUrl: "", lastSignInAt: "2026-10-26T10:00:00" } }));
  render(<AuthProvider><Probe /></AuthProvider>);
  await waitFor(() => expect(mocks.profile).toHaveBeenCalledWith(7));
  // when
  await act(async () => commitSession({ memberId: 8, role: "NORMAL" }));
  await screen.findByText("현재 회원");
  await act(async () => resolve({ data: { id: 7, nickname: "이전 회원", email: "old@example.com", profileImageUrl: "", lastSignInAt: "2026-10-26T10:00:00" } }));
  // then
  expect(screen.getByLabelText("프로필")).toHaveTextContent("현재 회원");
});
