import { expect, test } from "../fixtures";

test("Discord 인증을 마치면 서버 쿠키로 로그인하고 원래 페이지로 이동할 것이다", async ({ page, context }) => {
  // given
  let authorizationUrl = "";
  await page.route("**/bff/oauth2/authorization/discord", async route => {
    const response = await route.fetch({ maxRedirects: 0 });
    authorizationUrl = response.headers().location ?? "";
    await route.fulfill({
      status: 200,
      contentType: "text/html; charset=utf-8",
      headers: { "set-cookie": response.headers()["set-cookie"] ?? "" },
      body: "<h1>테스트 OAuth 공급자</h1>",
    });
  });
  await page.goto("/login?next=%2Fwatches");
  let callbacks = 0;
  page.on("request", request => { if (new URL(request.url()).pathname === "/bff/oauth2/callback/discord") callbacks += 1; });
  await page.getByRole("button", { name: "Discord로 시작하기" }).click();
  await expect(page.getByRole("heading", { name: "테스트 OAuth 공급자" })).toBeVisible();
  expect(authorizationUrl.startsWith("https://discord.com/oauth2/authorize?")).toBe(true);
  const state = new URL(authorizationUrl).searchParams.get("state");
  expect(state).toBeTruthy();
  // when
  await page.goto(`/bff/oauth2/callback/discord?code=fixture-code&state=${state}`);
  // then
  await expect(page).toHaveURL(/\/watches$/);
  await expect(page.getByRole("heading", { name: "채용 공고" })).toBeVisible();
  expect(callbacks).toBe(1);
  const cookie = (await context.cookies()).find(item => item.name === "JSESSIONID");
  expect(cookie?.httpOnly).toBe(true);
  expect((await page.evaluate(() => document.cookie)).includes("JSESSIONID")).toBe(false);
  expect((await context.cookies()).some(item => item.name.includes("watchtower_jwt"))).toBe(false);
});
test("로그인을 시작하지 않은 브라우저로 Discord 콜백이 오면 로그인하지 않고 오류를 보여줄 것이다", async ({ page }) => {
  // when
  await page.goto("/bff/oauth2/callback/discord?code=fixture-code&state=forged");
  // then
  await expect(page).toHaveURL(/\/login\/callback$/);
  await expect(page.getByRole("alert").filter({ hasText: "로그인 요청이 유효하지 않거나 만료" })).toBeVisible();
});
