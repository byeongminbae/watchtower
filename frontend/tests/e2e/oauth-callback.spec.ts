import { expect, test } from "../fixtures";

test("네이버 콜백을 처리하면 서버 쿠키로 로그인하고 원래 페이지로 이동할 것이다", async ({ page, context }) => {
  // given
  await page.route("https://nid.naver.com/**", route => route.fulfill({ status: 200, contentType: "text/html", body: "<h1>테스트 OAuth 공급자</h1>" }));
  await page.goto("/login?next=%2Fwatches");
  let exchanges = 0;
  page.on("request", request => { if (new URL(request.url()).pathname === "/bff/auth/naver/callback") exchanges += 1; });
  await page.getByRole("button", { name: "네이버로 시작하기" }).click();
  await page.waitForURL("https://nid.naver.com/**");
  const state = new URL(page.url()).searchParams.get("state");
  expect(state).toBeTruthy();
  // when
  await page.goto(`/login/callback?code=fixture-code&state=${state}`);
  // then
  await expect(page).toHaveURL(/\/watches$/);
  await expect(page.getByRole("heading", { name: "채용 공고" })).toBeVisible();
  expect(exchanges).toBe(1);
  const cookie = (await context.cookies()).find(item => item.name === "JSESSIONID");
  expect(cookie?.httpOnly).toBe(true);
  expect((await page.evaluate(() => document.cookie)).includes("JSESSIONID")).toBe(false);
  expect((await context.cookies()).some(item => item.name.includes("watchtower_jwt"))).toBe(false);
});
test("유효한 로그인 요청 없이 콜백에 접근하면 교환을 요청하지 않을 것이다", async ({ page }) => {
  // given
  let exchanges = 0;
  page.on("request", request => { if (new URL(request.url()).pathname === "/bff/auth/naver/callback") exchanges += 1; });
  // when
  await page.goto("/login/callback?code=fixture-code&state=forged");
  // then
  await expect(page.getByRole("alert").filter({ hasText: "로그인 요청이 유효하지 않거나 만료" })).toBeVisible();
  expect(exchanges).toBe(0);
});
