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
  await expect(page.getByRole("button", { name: "키워드 1개 감지 조건 관리" })).toBeVisible();
  await expect(page.getByText("이미지 변화 감지", { exact: true })).toBeVisible();
  await expect(page.getByText("영역: .job-list", { exact: true })).toBeVisible();
  await expect(page.getByText("페이지를 확인하고 있어요", { exact: true })).toBeVisible();
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
  await page.getByRole("button", { name: /감지 조건 관리$/ }).click();
  const conditionsDialog = page.getByRole("dialog", { name: "감지 조건", exact: true });
  await expect(conditionsDialog.getByText("사라질 때 알림", { exact: true })).toBeVisible();
  await conditionsDialog.getByRole("button", { name: "조건 수정", exact: true }).click();
  const conditionEditDialog = page.getByRole("dialog", { name: "감지 조건 수정", exact: true });
  await conditionEditDialog.getByRole("textbox", { name: /키워드/ }).fill("다시 모집");
  await conditionEditDialog.getByRole("switch", { name: "사라질 때 알림" }).click();
  await conditionEditDialog.getByRole("button", { name: "저장", exact: true }).click();
  // then
  await expect(conditionsDialog.getByText("키워드: 다시 모집", { exact: true })).toBeVisible();
  await expect(conditionsDialog.getByText("등장할 때 알림", { exact: true })).toBeVisible();
  // when
  await page.keyboard.press("Escape");
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
  await expect(page.getByText("페이지를 확인하고 있어요", { exact: true })).toBeVisible();
  await expect(page.getByText("페이지를 확인하지 못했어요", { exact: true })).toHaveCount(0);
  const records = await (await request.get(`http://127.0.0.1:4100/__test/records?session=${session}`)).json();
  expect(records.data).toContainEqual(expect.objectContaining({ method: "POST", path: "/api/v1/towers/9/crawl-retry", csrf: "csrf-fixture" }));
});

test("변화 기록 히트맵은 처음에 가장 최근 변화가 감지된 시간대를 골라 그 시간의 기록만 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  // when
  await page.goto("/watches/9");
  // then
  await expect(page.getByRole("button", { name: /변화 1건$/, pressed: true })).toBeVisible();
  await expect(page.getByRole("button", { name: /신규 채용 공고/ })).toBeVisible();
  await expect(page.getByRole("button", { name: /마감된 공고 정리/ })).toHaveCount(0);
});

test("변화 기록 히트맵에서 다른 시간대 칸을 누르면 그 시간의 기록으로 바꿔 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches/9");
  // when
  await page.getByRole("button", { name: /변화 1건$/, pressed: false }).click();
  // then
  await expect(page.getByRole("button", { name: /마감된 공고 정리/ })).toBeVisible();
  await expect(page.getByRole("button", { name: /신규 채용 공고/ })).toHaveCount(0);
});

test("모바일 화면에서는 변화 기록 히트맵 칸을 3시간 단위로 묶어 누르기 쉽게 보여줄 것이다", async ({ page, login }) => {
  // given
  await page.setViewportSize({ width: 390, height: 844 });
  await login();
  // when
  await page.goto("/watches/9");
  // then
  const selectedSlot = page.getByRole("button", { name: /\d+~\d+시 변화 1건$/, pressed: true });
  await expect(selectedSlot).toBeVisible();
  const { width, height } = (await selectedSlot.boundingBox())!;
  expect(width).toBeGreaterThanOrEqual(24);
  expect(height).toBeGreaterThanOrEqual(24);
});

test("변화 기록 오른쪽에 변경 부분 스크린샷을 작게 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  // when
  await page.goto("/watches/9");
  // then
  await expect(page.getByRole("button", { name: /신규 채용 공고/ }).locator("img")).toHaveAttribute("src", "/bff/towers/9/snapshots/1/screenshot");
});

test("변화 기록을 누르면 창에서 감지 시간과 알림 시간과 AI 요약을 스크린샷과 나눠 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches/9");
  // when
  await page.getByRole("button", { name: /신규 채용 공고/ }).click();
  // then
  const dialog = page.getByRole("dialog", { name: "변화 상세" });
  await expect(dialog.getByRole("term")).toHaveText(["감지 시간", "알림 시간"]);
  await expect(dialog.getByRole("definition")).toHaveText([/^\d+월 \d+일 \d{2}:\d{2}$/, /^\d+월 \d+일 \d{2}:\d{2}$/]);
  const aiSummary = dialog.getByRole("alert");
  await expect(aiSummary.getByText("AI 요약", { exact: true })).toBeVisible();
  await expect(aiSummary.getByText("신규 채용 공고", { exact: true })).toBeVisible();
});

test("알림을 보내지 않은 변화 기록을 누르면 창에서 알림을 보내지 않았다고 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches/9");
  await page.getByRole("button", { name: /변화 1건$/, pressed: false }).click();
  // when
  await page.getByRole("button", { name: /마감된 공고 정리/ }).click();
  // then
  await expect(page.getByRole("dialog", { name: "변화 상세" }).getByRole("definition").last()).toHaveText("보내지 않았어요");
});

test("변화 기록을 누르면 다른 화면으로 이동하지 않고 창에서 변경 부분 스크린샷을 크게 보여줄 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches/9");
  // when
  await page.getByRole("button", { name: /신규 채용 공고/ }).click();
  // then
  const dialog = page.getByRole("dialog", { name: "변화 상세" });
  await expect(dialog.getByRole("img", { name: "변경 부분을 표시한 스크린샷 원본" })).toHaveJSProperty("complete", true);
  await expect(page).toHaveURL(/\/watches\/9$/);
  expect(page.context().pages()).toHaveLength(1);
  const { clientHeight, scrollHeight } = await dialog.evaluate(content => ({ clientHeight: content.clientHeight, scrollHeight: content.scrollHeight }));
  expect(scrollHeight).toBeGreaterThan(clientHeight);
});
