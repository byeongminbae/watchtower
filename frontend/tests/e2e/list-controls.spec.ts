import { expect, test } from "../fixtures";

for (const { route, endpoint, label, query } of [
  { route: "/watches", endpoint: "/bff/member/7/towers", label: "타워 이름 접두사 검색", query: "채용" },
  { route: "/mypage/notifications", endpoint: "/bff/notification/channels", label: "채널 이름 접두사 검색", query: "개발" }
]) {
  test(`${route} 목록은 아이콘 메뉴로 정렬하고 검색해도 페이지 크기를 고정할 것이다`, async ({ page, login }, info) => {
    // given
    await login();
    const requests: URL[] = [];
    page.on("request", request => { const url = new URL(request.url()); if (url.pathname === endpoint) requests.push(url); });
    await page.goto(route);
    await expect(page.getByRole("status")).toHaveCount(0);
    await expect(page.getByLabel("페이지 크기")).toHaveCount(0);
    // when
    await page.getByRole("button", { name: "정렬 기준", exact: true }).click();
    await expect(page.getByRole("menuitemradio", { name: "등록 순서", exact: true })).toHaveAttribute("aria-checked", "true");
    await page.screenshot({ animations: "disabled", path: info.outputPath(`${route === "/watches" ? "tower" : "channel"}-sort-menu.png`) });
    const sorted = page.waitForRequest(request => { const url = new URL(request.url()); return url.pathname === endpoint && url.searchParams.get("sortField") === "UPDATED_AT"; });
    await page.getByRole("menuitemradio", { name: "수정일", exact: true }).click();
    await sorted;
    await page.getByRole("button", { name: "정렬 방향", exact: true }).click();
    const ascending = page.waitForRequest(request => { const url = new URL(request.url()); return url.pathname === endpoint && url.searchParams.get("sortDirection") === "ASC"; });
    await page.getByRole("menuitemradio", { name: "오름차순", exact: true }).click();
    await ascending;
    await page.getByLabel(label).fill(query);
    const searched = page.waitForRequest(request => { const url = new URL(request.url()); return url.pathname === endpoint && url.searchParams.get("query") === query; });
    await page.getByRole("button", { name: "검색", exact: true }).click();
    await searched;
    // then
    await expect(page.getByRole("status")).toHaveCount(0);
    expect(requests.length).toBeGreaterThanOrEqual(4);
    const pageSize = requests[0].searchParams.get("size");
    expect(pageSize).not.toBeNull();
    expect(requests.every(url => url.searchParams.get("size") === pageSize)).toBe(true);
    const last = requests[requests.length - 1];
    expect(last.searchParams.get("sortField")).toBe("UPDATED_AT");
    expect(last.searchParams.get("sortDirection")).toBe("ASC");
    expect(last.searchParams.has("cursorId")).toBe(false);
    if (route === "/watches") {
      await expect(page.getByText("타워 목록", { exact: true }).locator("..").locator("..").getByLabel(label)).toBeVisible();
      await expect(page.getByRole("link", { name: "상세 링크" })).toHaveCount(0);
    }
  });
}

test("제목 옆 아이콘으로 감시를 멈추고 다시 시작하면 기존 타워 설정을 유지할 것이다", async ({ page, login, request }) => {
  // given
  const session = await login();
  await page.goto("/watches");
  await expect(page.getByRole("status")).toHaveCount(0);
  // when
  await page.getByRole("button", { name: "감시 일시정지", exact: true }).click();
  // then
  await expect(page.getByRole("button", { name: "감시 시작", exact: true })).toBeVisible();
  await expect(page.getByText("감시가 멈춰 있어요")).toBeVisible();
  // when
  await page.getByRole("button", { name: "감시 시작", exact: true }).click();
  // then
  await expect(page.getByRole("button", { name: "감시 일시정지", exact: true })).toBeVisible();
  const records = await (await request.get(`http://127.0.0.1:4100/__test/records?session=${session}`)).json();
  for (const status of ["PAUSED", "RUNNING"]) {
    expect(records.data).toContainEqual(expect.objectContaining({ method: "PUT", path: "/api/v1/towers/9", body: { name: "채용 공고", fetchInterval: "PT5M", includeInStat: true, status }, csrf: "csrf-fixture" }));
  }
});

test("감시 상태 변경이 실패하면 기존 상태를 유지하고 오류를 표시할 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches");
  await expect(page.getByRole("status")).toHaveCount(0);
  await page.route("**/bff/towers/9", route => route.request().method() === "PUT" ? route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ isSuccess: false, statusCode: "COM_001", message: "상태 변경 실패", timestamp: "2026-10-27T12:00:00" }) }) : route.continue());
  // when
  await page.getByRole("button", { name: "감시 일시정지", exact: true }).click();
  // then
  await expect(page.getByRole("alert").filter({ hasText: "상태 변경 실패" })).toBeVisible();
  await expect(page.getByRole("button", { name: "감시 일시정지", exact: true })).toBeEnabled();
  await expect(page.getByRole("button", { name: "감시 시작", exact: true })).toHaveCount(0);
});
