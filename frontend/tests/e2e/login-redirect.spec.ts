import { expect, test } from "../fixtures";

test("로그인한 회원이 로그인 주소를 열면 타워 화면으로 이동할 것이다", async ({ page, login }) => {
  // given
  await login();
  // when
  await page.goto("/login");
  // then
  await expect(page).toHaveURL(/\/watches$/);
});
test("관리자가 로그인 주소를 열면 관리자 대시보드로 이동할 것이다", async ({ page, login }) => {
  // given
  await login("ADMIN");
  // when
  await page.goto("/login");
  // then
  await expect(page).toHaveURL(/\/admin$/);
  await expect(page.getByRole("heading", { name: "관리자 대시보드" })).toBeVisible();
});
test("비로그인 회원이 보호된 페이지를 요청하면 로그인 화면으로 이동할 것이다", async ({ page }) => {
  // given
  // when
  await page.goto("/mypage");
  // then
  await expect(page).toHaveURL(/\/login\?next=%2Fmypage$/);
});
