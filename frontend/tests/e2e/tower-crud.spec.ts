import { expect, test } from "../fixtures";

test("소멸 조건으로 타워를 등록하고 수정과 삭제를 완료할 것이다", async ({ page, login, request }) => {
  // given
  const session = await login();
  await page.goto("/watches");
  await page.getByRole("button", { name: "타워 등록" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("사이트 이름").fill("새 타워");
  await dialog.getByLabel("감시할 URL").fill("https://example.com/new");
  await dialog.getByRole("switch", { name: "이미지 변화" }).click();
  await dialog.getByRole("textbox", { name: "감시할 영역 (선택)" }).fill(".job-list");
  await dialog.getByRole("switch", { name: "키워드 감지 조건도 함께 추가" }).click();
  await dialog.getByRole("textbox", { name: /키워드/ }).fill("모집 마감");
  await dialog.getByRole("switch", { name: "등장할 때 알림" }).click();
  await dialog.getByLabel("확인 주기 (분)").fill("10");
  // when
  await dialog.getByRole("button", { name: "등록", exact: true }).click();
  // then
  await expect(page.getByRole("heading", { name: "새 타워", exact: true })).toBeVisible();
  await expect(page.getByText("사라질 때 알림", { exact: true })).toBeVisible();
  await expect(page.getByText("이미지 변화 감지", { exact: true })).toBeVisible();
  await expect(page.getByText("영역: .job-list", { exact: true })).toBeVisible();
  await expect(page.getByText("지금 페이지를 확인하고 있어요", { exact: true })).toBeVisible();
  const records = await (await request.get(`http://127.0.0.1:4100/__test/records?session=${session}`)).json();
  expect(records.data).toContainEqual(expect.objectContaining({
    method: "POST", path: "/api/v1/towers", csrf: "csrf-fixture", body: {
      name: "새 타워", url: "https://example.com/new", fetchInterval: "PT10M", includeInStat: true,
      detectTextChange: true, detectImageChange: true, ignoreDateTime: false, detectionAreaSelector: ".job-list",
      conditions: [{ type: "KEYWORD", keyword: "모집 마감", satisfiedWhen: "DISAPPEAR" }]
    }
  }));
  // when
  await page.getByRole("button", { name: "타워 수정" }).click();
  await dialog.getByLabel("사이트 이름").fill("수정 타워");
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

test("크롤링에 실패한 타워를 다시 확인하면 확인 중으로 바뀌고 재시도를 요청할 것이다", async ({ page, login, request }) => {
  // given
  const session = await login();
  await request.get(`http://127.0.0.1:4100/__test/fail-crawl?session=${session}&towerId=9`);
  await page.goto("/watches/9");
  await expect(page.getByText("페이지를 확인하지 못했어요", { exact: true })).toBeVisible();
  // when
  await page.getByRole("button", { name: "다시 확인", exact: true }).click();
  // then
  await expect(page.getByText("지금 페이지를 확인하고 있어요", { exact: true })).toBeVisible();
  await expect(page.getByText("페이지를 확인하지 못했어요", { exact: true })).toHaveCount(0);
  const records = await (await request.get(`http://127.0.0.1:4100/__test/records?session=${session}`)).json();
  expect(records.data).toContainEqual(expect.objectContaining({ method: "POST", path: "/api/v1/towers/9/crawl-retry", csrf: "csrf-fixture" }));
});

test("변화 기록 탭을 열면 감지된 변경의 감지 시각과 요약과 변경 부분 스크린샷을 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches/9");
  // when
  await page.getByRole("tab", { name: /변화 기록/ }).click();
  // then
  await expect(page.getByText("신규 채용 공고", { exact: true })).toBeVisible();
  await expect(page.locator("time[datetime='2026-11-03T20:55:38Z']")).toHaveAttribute("title", /2026/);
  await expect(page.getByRole("img", { name: "변경 부분을 표시한 스크린샷" })).toHaveAttribute("src", "https://objectstorage.ap-osaka-1.oraclecloud.com/p/token/o/1.png");
});

test("변경 부분 스크린샷은 작게 보여주고 누르면 다른 화면으로 이동하지 않고 창에서 크게 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.route("https://objectstorage.ap-osaka-1.oraclecloud.com/**", route => route.fulfill({
    contentType: "image/svg+xml",
    body: "<svg xmlns='http://www.w3.org/2000/svg' width='1280' height='10000'><rect width='1280' height='10000' fill='white'/></svg>"
  }));
  await page.goto("/watches/9");
  await page.getByRole("tab", { name: /변화 기록/ }).click();
  const preview = page.getByRole("button", { name: "변경 부분 스크린샷 크게 보기" });
  await expect(preview.getByRole("img")).toHaveJSProperty("complete", true);
  expect((await preview.boundingBox())?.height).toBeLessThanOrEqual(160);
  // when
  await preview.click();
  // then
  const dialog = page.getByRole("dialog", { name: "변경 부분 스크린샷" });
  await expect(dialog.getByRole("img", { name: "변경 부분을 표시한 스크린샷 원본" })).toHaveJSProperty("complete", true);
  await expect(page).toHaveURL(/\/watches\/9$/);
  expect(page.context().pages()).toHaveLength(1);
  const { clientHeight, scrollHeight } = await dialog.evaluate(content => ({ clientHeight: content.clientHeight, scrollHeight: content.scrollHeight }));
  expect(scrollHeight).toBeGreaterThan(clientHeight);
});
