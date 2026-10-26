import { expect, test } from "../fixtures";

test("서버에서 세션이 만료되면 새로고침할 때 로그인 화면으로 이동할 것이다", async ({ page, login, request }) => {
  // given
  const session = await login();
  await page.goto("/watches");
  await expect(page.getByRole("heading", { name: "채용 공고" })).toBeVisible();
  // when
  await request.post(`http://127.0.0.1:4100/__test/invalidate?session=${session}`);
  await page.reload();
  // then
  await expect(page).toHaveURL(/\/login\?next=%2Fwatches$/);
  await expect(page.getByRole("button", { name: "네이버로 시작하기" })).toBeVisible();
});
test("로그아웃하면 서버 세션 쿠키를 제거하고 홈으로 이동할 것이다", async ({ page, login, context }) => {
  // given
  await login();
  await page.goto("/watches");
  await page.getByRole("button", { name: "테스트 회원 메뉴" }).click();
  // when
  await page.getByRole("menuitem", { name: "로그아웃" }).click();
  // then
  await expect(page).toHaveURL("http://127.0.0.1:3000/");
  expect((await context.cookies()).some(cookie => cookie.name === "JSESSIONID")).toBe(false);
});
