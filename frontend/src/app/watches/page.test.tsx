import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import WatchesPage from "./page";
import { ApiError } from "@/lib/api/errors";
const list = vi.hoisted(() => vi.fn());
vi.mock("@/lib/api", () => ({ watchApi: { listMemberWatches: list } }));
vi.mock("@/lib/AuthContext", () => ({ useAuth: () => ({ principal: { memberId: 7, role: "NORMAL" } }) }));
beforeEach(() => { list.mockReset(); });
it("타워 목록 요청이 진행 중이면 빈 목록을 표시하지 않을 것이다", async () => {
  // given
  list.mockReturnValue(new Promise(() => undefined));
  // when
  render(<WatchesPage />);
  // then
  expect(screen.getByRole("status")).toHaveTextContent("타워 불러오는 중");
  expect(screen.queryByText(/등록된 타워가 없어/)).not.toBeInTheDocument();
  await waitFor(() => expect(list).toHaveBeenCalledWith(7, expect.objectContaining({ sortField: "ID", sortDirection: "DESC" })));
});
it("서버가 빈 페이지를 반환하면 등록 안내를 표시할 것이다", async () => {
  // given
  list.mockResolvedValue({ data: { data: [], hasNext: false, nextCursorId: null, nextCursorValue: null, size: 20 } });
  // when
  render(<WatchesPage />);
  // then
  expect(await screen.findByText("등록된 타워가 없어요. 타워를 등록해 주세요.")).toBeVisible();
  expect(screen.getByRole("button", { name: "타워 등록" })).toBeVisible();
});
it("목록 조회가 실패하면 서버 오류와 재시도를 표시할 것이다", async () => {
  // given
  list.mockRejectedValue(new ApiError("조회 실패", 502));
  // when
  render(<WatchesPage />);
  // then
  expect(await screen.findByRole("alert")).toHaveTextContent("조회 실패");
  expect(screen.getByRole("button", { name: "재시도" })).toBeVisible();
});
