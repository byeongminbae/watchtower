import { expect, test } from "../fixtures";

test("알림 채널을 등록하고 이름을 수정한 뒤 삭제할 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/mypage/notifications");
  await page.getByRole("button", { name: "채널 등록" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("채널 이름").fill("새 채널");
  await dialog.getByLabel("채널 종류").click();
  await page.getByRole("option", { name: "Telegram", exact: true }).click();
  await expect(dialog.getByLabel("Webhook URL")).toHaveAttribute("type", "url");
  await dialog.getByLabel("Webhook URL").fill("https://example.com/hooks/telegram");
  // when
  await dialog.getByRole("button", { name: "저장", exact: true }).click();
  // then
  await expect(page.getByRole("heading", { name: "새 채널" })).toBeVisible();
  await expect(page.getByText("Telegram으로 변화를 알려드려요.")).toHaveCount(0);
  // when
  await page.getByRole("heading", { name: "새 채널" }).locator("..").getByRole("button", { name: "수정", exact: true }).click();
  await expect(dialog.getByLabel("채널 종류")).toBeDisabled();
  await dialog.getByLabel("채널 이름").fill("수정 채널");
  await dialog.getByRole("button", { name: "저장", exact: true }).click();
  // then
  await expect(page.getByRole("heading", { name: "수정 채널" })).toBeVisible();
  // when
  await page.getByRole("heading", { name: "수정 채널" }).locator("..").getByRole("button", { name: "삭제", exact: true }).click();
  await page.getByRole("alertdialog").getByRole("button", { name: "삭제", exact: true }).click();
  // then
  await expect(page.getByRole("heading", { name: "수정 채널" })).toHaveCount(0);
});

test("관리자는 탈퇴 회원도 조회하고 정지 사유를 입력해 회원을 정지할 것이다", async ({ page, login, request }) => {
  // given
  const session = await login("ADMIN");
  await page.goto("/admin/members");
  await expect(page.getByText("탈퇴 회원", { exact: true })).toBeVisible();
  const row = page.getByRole("row").filter({ hasText: "테스트 회원" });
  await row.getByRole("button", { name: "정지", exact: true }).click();
  await page.getByLabel("정지 사유").fill("정책 위반 반복");
  // when
  await page.getByRole("dialog").getByRole("button", { name: "확인", exact: true }).click();
  // then
  await expect(row.getByText("정지 사유: 정책 위반 반복", { exact: true })).toBeVisible();
  const records = await (await request.get(`http://127.0.0.1:4100/__test/records?session=${session}`)).json();
  expect(records.data).toContainEqual(expect.objectContaining({
    method: "PUT", path: "/api/v1/admin/members/7/status", body: { banned: true, banReason: "정책 위반 반복" }, csrf: "csrf-fixture"
  }));
  // when
  await row.getByRole("button", { name: "정지 해제", exact: true }).click();
  await page.getByRole("dialog").getByRole("button", { name: "확인", exact: true }).click();
  // then
  await expect(row.getByRole("button", { name: "정지", exact: true })).toBeVisible();
});
