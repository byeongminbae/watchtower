import { expect, test } from "../fixtures";

const CALLBACK_PATH = "/bff/auth/naver/callback";
const TIMESTAMP = "2026-07-29T10:00:00";

test("Given a mounted protected shell, when an in-flight callback returns 401, then header and guard log out without reload", async ({
  page,
  oauth,
}, testInfo) => {
  // Given
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  await oauth.storeTransaction(page, "bootstrap-state");
  let releaseCallback = (): void => undefined;
  const callbackGate = new Promise<void>((resolve) => {
    releaseCallback = resolve;
  });
  await page.route(`**${CALLBACK_PATH}?**`, async (route) => {
    const code = new URL(route.request().url()).searchParams.get("code");
    if (code === "bootstrap-code") {
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          success: true,
          data: oauth.validTokens(),
          timestamp: TIMESTAMP,
        }),
      });
      return;
    }
    await callbackGate;
    await route.fulfill({
      status: 401,
      contentType: "application/json",
      body: "not-json",
    });
  });
  await page.goto(
    "/login/callback?code=bootstrap-code&state=bootstrap-state",
  );
  await expect(page).toHaveURL(/\/watches$/u);
  await page.reload();
  await expect(page).toHaveURL(/\/watches$/u);
  await expect(page.getByRole("button", { name: "회원 메뉴" })).toBeVisible();
  await expect(
    page.getByText("백엔드 기능이 아직 제공되지 않습니다."),
  ).toBeVisible();
  await page.evaluate(() => {
    sessionStorage.setItem(
      "watchtower_oauth_transaction",
      JSON.stringify({
        nonce: "fixture-state",
        returnPath: "/watches",
        createdAt: Date.now(),
      }),
    );
  });
  await page.goto(
    "/login/callback?code=fixture-code&state=fixture-state",
  );
  await expect(page.getByRole("button", { name: "회원 메뉴" })).toBeVisible();
  await page.getByRole("link", { name: "내 와치리스트" }).click();
  await expect(page).toHaveURL(/\/watches$/u);
  await expect(
    page.getByText("백엔드 기능이 아직 제공되지 않습니다."),
  ).toBeVisible();

  // When
  releaseCallback();

  // Then
  await expect(page.getByRole("link", { name: "로그인" })).toBeVisible();
  await expect(page).toHaveURL(/\/login\?next=%2Fwatches$/u);
  const sessionCookies = (await page.context().cookies()).filter((cookie) =>
    cookie.name.startsWith("watchtower_"),
  );
  expect(sessionCookies).toEqual([]);
  expect(pageErrors).toEqual([]);
  await page.screenshot({
    path: testInfo.outputPath("unauthorized-immediate-logout.png"),
    fullPage: true,
  });
});
