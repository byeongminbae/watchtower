import { expect, test } from "../fixtures";

test("채널과 소멸 조건으로 타워를 등록하고 수정과 삭제를 완료할 것이다", async ({ page, login, request }) => {
  // given
  const session = await login();
  await page.goto("/watches");
  await page.getByRole("button", { name: "타워 등록" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("사이트 이름").fill("새 타워");
  await dialog.getByLabel("감시할 URL").fill("https://example.com/new");
  await dialog.getByLabel("감지 조건").click();
  await page.getByRole("option", { name: "키워드 감지", exact: true }).click();
  await dialog.getByRole("textbox", { name: /키워드/ }).fill("모집 마감");
  await dialog.getByRole("switch", { name: "등장할 때 알림" }).click();
  await dialog.getByLabel("확인 주기 (분)").fill("10");
  await dialog.getByRole("checkbox", { name: "개발 알림 (DISCORD)" }).check();
  // when
  await dialog.getByRole("button", { name: "등록", exact: true }).click();
  // then
  await expect(page.getByRole("heading", { name: "새 타워", exact: true })).toBeVisible();
  await expect(page.getByText("사라질 때 알림", { exact: true })).toBeVisible();
  const records = await (await request.get(`http://127.0.0.1:4100/__test/records?session=${session}`)).json();
  expect(records.data).toContainEqual(expect.objectContaining({
    method: "POST", path: "/api/v1/towers", csrf: "csrf-fixture", body: {
      name: "새 타워", url: "https://example.com/new", fetchInterval: "PT10M", includeInStat: true, notificationChannelIds: [3],
      conditions: [{ type: "KEYWORD", keyword: "모집 마감", isSatisfiedAtAppearance: false }]
    }
  }));
  // when
  await page.getByRole("button", { name: "타워 수정" }).click();
  await dialog.getByLabel("사이트 이름").fill("수정 타워");
  await dialog.getByLabel("상태", { exact: true }).click();
  await page.getByRole("option", { name: "일시정지", exact: true }).click();
  await dialog.getByRole("button", { name: "저장", exact: true }).click();
  // then
  await expect(page.getByRole("heading", { name: "수정 타워", exact: true })).toBeVisible();
  // when
  await page.getByRole("button", { name: "조건 수정", exact: true }).click();
  await dialog.getByRole("textbox", { name: /키워드/ }).fill("다시 모집");
  await dialog.getByRole("switch", { name: "사라질 때 알림" }).click();
  await dialog.getByRole("button", { name: "저장", exact: true }).click();
  // then
  await expect(page.getByText("키워드: 다시 모집", { exact: true })).toBeVisible();
  await expect(page.getByText("등장할 때 알림", { exact: true })).toBeVisible();
  // when
  await page.getByRole("button", { name: "타워 삭제", exact: true }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "삭제", exact: true }).click();
  // then
  await expect(page.getByRole("heading", { name: "수정 타워", exact: true })).toHaveCount(0);
});

test("스냅샷 보기를 전환하면 실제 HTML 링크를 변경할 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches/9");
  // when
  await page.getByRole("button", { name: "전체 페이지", exact: true }).click();
  // then
  await expect(page.getByRole("link", { name: "HTML 열기" })).toHaveAttribute("href", "https://example.com/full.html");
  // when
  await page.getByRole("button", { name: "변경 부분", exact: true }).click();
  // then
  await expect(page.getByRole("link", { name: "HTML 열기" })).toHaveAttribute("href", "https://example.com/diff.html");
});
