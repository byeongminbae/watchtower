import { expect, test } from "../fixtures";

const BFCACHE_RESTORED_MARKER = "watchtower:test:bfcache-restored";

test.describe("Authenticated login route", () => {
  test.describe("before hydration", () => {
    test.use({ javaScriptEnabled: false });

    test("Given valid session cookies, when login is requested before hydration, then the server redirects to the root page", async ({
      page,
      oauth,
      baseURL,
    }) => {
      // Given
      if (baseURL === undefined) throw new Error("Playwright baseURL is required");
      const tokens = oauth.validTokens();
      await page.context().addCookies([
        { name: "watchtower_jwt", value: tokens.accessToken, url: baseURL },
        { name: "watchtower_refresh", value: tokens.refreshToken, url: baseURL },
      ]);

      // When
      await page.goto("/login");

      // Then
      await expect(page).toHaveURL(`${baseURL}/`);
      await expect(page.getByRole("progressbar")).toHaveCount(0);
    });
  });

  test("Given an anonymous login page in browser history, when Back restores it after authentication, then replaces it with the root page", async ({
    page,
    oauth,
    baseURL,
  }) => {
    // Given
    if (baseURL === undefined) throw new Error("Playwright baseURL is required");
    let restoredFromBackForwardCache = false;
    page.on("console", (message) => {
      if (message.text() === BFCACHE_RESTORED_MARKER) restoredFromBackForwardCache = true;
    });
    await page.addInitScript((marker) => {
      window.addEventListener("pageshow", (event) => {
        if (event.persisted) console.debug(marker);
      });
    }, BFCACHE_RESTORED_MARKER);
    await page.goto("/login");
    await expect(page.getByRole("button", { name: "네이버로 시작하기" })).toBeVisible();
    await page.goto("/");
    const tokens = oauth.validTokens();
    await page.context().addCookies([
      { name: "watchtower_jwt", value: tokens.accessToken, url: baseURL },
      { name: "watchtower_refresh", value: tokens.refreshToken, url: baseURL },
    ]);

    // When
    await page.evaluate(() => history.back());

    // Then
    await expect.poll(() => restoredFromBackForwardCache).toBe(true);
    await expect(page).toHaveURL(`${baseURL}/`);
    await expect(page.getByRole("progressbar")).toHaveCount(0);
  });
});
