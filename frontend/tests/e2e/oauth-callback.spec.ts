import { expect, test } from "../fixtures";

const TIMESTAMP = "2026-07-29T10:00:00";
const CALLBACK_PATH = "/api/v1/auth/naver/callback";

function successEnvelope(data: unknown): string {
  return JSON.stringify({ success: true, data, timestamp: TIMESTAMP });
}

test.describe("OAuth callback desktop", () => {
  test.use({ viewport: { width: 1440, height: 900 } });

  test("Given a Naver login initiation, when the frontend callback succeeds, then session survives protected navigation and reload", async ({
    page,
    oauth,
  }, testInfo) => {
    // Given
    const consoleMessages: string[] = [];
    page.on("console", (message) => consoleMessages.push(message.text()));
    await page.addInitScript(() => {
      const startObserver = (): void => {
        const inspect = (): void => {
          if (document.body?.textContent?.includes("아직 등록된 와치리스트가 없어요")) {
            document.documentElement.setAttribute("data-false-empty-observed", "true");
          }
        };
        document.documentElement.setAttribute("data-false-empty-observed", "false");
        new MutationObserver(inspect).observe(document.documentElement, {
          childList: true,
          subtree: true,
          characterData: true,
        });
        inspect();
      };
      document.addEventListener("DOMContentLoaded", startObserver, { once: true });
    });
    let issuedState = "";
    let releaseCallback = (): void => undefined;
    const callbackGate = new Promise<void>((resolve) => {
      releaseCallback = resolve;
    });
    await page.route("**/api/v1/auth/naver/url?**", async (route) => {
      issuedState = new URL(route.request().url()).searchParams.get("state") ?? "";
      await route.fulfill({
        contentType: "application/json",
        body: successEnvelope(
          `https://nid.naver.com/oauth2.0/authorize?response_type=code&state=${encodeURIComponent(issuedState)}`,
        ),
      });
    });
    await page.route("https://nid.naver.com/**", (route) =>
      route.fulfill({ contentType: "text/html", body: "<main>Naver OAuth</main>" }),
    );
    await page.route(`**${CALLBACK_PATH}?**`, async (route) => {
      await callbackGate;
      await route.fulfill({
        contentType: "application/json",
        body: successEnvelope(oauth.validTokens()),
      });
    });
    await page.route("**/api/v1/member/7", (route) =>
      route.fulfill({
        contentType: "application/json",
        body: successEnvelope({
          id: 7,
          nickname: "실제 감시자",
          email: "watcher@example.com",
          profileImageUrl: "",
          lastSignInAt: TIMESTAMP,
        }),
      }),
    );
    await page.goto("/watches?tab=mine#changes");
    await expect(page).toHaveURL(
      /\/login\?next=%2Fwatches%3Ftab%3Dmine%23changes$/u,
    );

    // When
    await page.getByRole("button", { name: "네이버로 시작하기" }).click();
    await page.waitForURL("https://nid.naver.com/**");
    expect(issuedState).not.toBe("");
    await page.goto(
      `/login/callback?code=fixture-code&state=${encodeURIComponent(issuedState)}`,
    );
    await expect(page.getByRole("status")).toContainText("확인하고 있습니다");
    await page.screenshot({
      path: testInfo.outputPath("desktop-loading.png"),
    });
    releaseCallback();

    // Then
    await expect(page).toHaveURL(/\/watches\?tab=mine#changes$/u);
    await expect(
      page.getByRole("button", { name: "실제 감시자 메뉴" }),
    ).toBeVisible();
    await expect(
      page.getByText("백엔드 기능이 아직 제공되지 않습니다."),
    ).toBeVisible();
    expect(oauth.callbackRequestCount()).toBe(1);
    const sessionCookies = (await page.context().cookies()).filter((cookie) =>
      cookie.name.startsWith("watchtower_"),
    );
    expect(sessionCookies.map((cookie) => cookie.name).sort()).toEqual([
      "watchtower_jwt",
      "watchtower_refresh",
    ]);
    await page.reload();
    await expect(page).toHaveURL(/\/watches\?tab=mine#changes$/u);
    expect(page.url()).not.toContain("/login?");
    await expect(
      page.getByRole("button", { name: "실제 감시자 메뉴" }),
    ).toBeVisible();
    await expect(
      page.getByText("백엔드 기능이 아직 제공되지 않습니다."),
    ).toBeVisible();
    expect(consoleMessages.join("\n")).not.toContain(issuedState);
    await expect(page.locator("html")).toHaveAttribute("data-false-empty-observed", "false");
    await page.screenshot({
      path: testInfo.outputPath("desktop-success.png"),
      fullPage: true,
    });
    const rootURL = new URL("/", page.url()).toString();
    await page.goto("/login");
    await expect(page).toHaveURL(rootURL);
  });

  test("Given a provider rejection, when callback loads, then query is removed and no exchange or cookie occurs", async ({
    page,
    oauth,
  }, testInfo) => {
    // Given
    await oauth.storeTransaction(page, "fixture-state");

    // When
    await page.goto(
      "/login/callback?error=access_denied&error_description=provider-secret&state=fixture-state",
    );

    // Then
    await expect(
      page.getByRole("alert").filter({ hasText: "취소되었거나 거부" }),
    ).toBeVisible();
    await expect(page).toHaveURL(/\/login\/callback$/u);
    expect(page.url()).not.toContain("provider-secret");
    expect(oauth.callbackRequestCount()).toBe(0);
    expect(await page.context().cookies()).toEqual([]);
    await page.screenshot({
      path: testInfo.outputPath("desktop-provider-error.png"),
    });
  });

  for (const fixture of [
    { name: "missing code", query: "?state=fixture-state", state: "fixture-state" },
    { name: "missing state", query: "?code=fixture-code", state: "fixture-state" },
    { name: "mismatched state", query: "?code=fixture-code&state=wrong", state: "fixture-state" },
    { name: "replayed state", query: "?code=fixture-code&state=fixture-state", state: null },
  ]) {
    test(`Given ${fixture.name}, when callback loads, then it fails before exchange`, async ({
      page,
      oauth,
    }, testInfo) => {
      // Given
      if (fixture.state !== null) {
        await oauth.storeTransaction(page, fixture.state);
      } else {
        await page.goto("/login");
      }

      // When
      await page.goto(`/login/callback${fixture.query}`);

      // Then
      await expect(
        page.getByRole("alert").filter({ hasText: "유효하지 않거나 만료" }),
      ).toBeVisible();
      await expect(page).toHaveURL(/\/login\/callback$/u);
      expect(oauth.callbackRequestCount()).toBe(0);
      expect(await page.context().cookies()).toEqual([]);
      if (fixture.name === "missing code") {
        await page.screenshot({
          path: testInfo.outputPath("desktop-invalid-state.png"),
        });
      }
    });
  }

  test("Given an unsafe stored return target, when callback loads, then it rejects before exchange", async ({
    page,
    oauth,
  }) => {
    // Given
    await oauth.storeTransaction(page, "fixture-state", "https://attacker.example");

    // When
    await page.goto("/login/callback?code=fixture-code&state=fixture-state");

    // Then
    await expect(
      page.getByRole("alert").filter({ hasText: "유효하지 않거나 만료" }),
    ).toBeVisible();
    await expect(page).toHaveURL(/\/login\/callback$/u);
    expect(page.url()).not.toContain("attacker.example");
    expect(oauth.callbackRequestCount()).toBe(0);
    expect(await page.context().cookies()).toEqual([]);
  });
});

test.describe("OAuth callback mobile failures", () => {
  test.use({ viewport: { width: 375, height: 812 } });

  for (const failure of [
    { name: "2xx backend error", kind: "error-envelope" },
    { name: "malformed token response", kind: "malformed" },
    { name: "invalid JWT pair", kind: "invalid-jwt" },
    { name: "network failure", kind: "network" },
  ]) {
    test(`Given ${failure.name}, when callback exchanges, then retry is accessible with zero partial cookies`, async ({
      page,
      oauth,
    }, testInfo) => {
      // Given
      await oauth.storeTransaction(page, "fixture-state");
      await page.route(`**${CALLBACK_PATH}?**`, (route) => {
        if (failure.kind === "network") return route.abort("failed");
        if (failure.kind === "error-envelope") {
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              success: false,
              statusCode: "OAUTH_FAILED",
              message: "backend-secret",
              timestamp: TIMESTAMP,
            }),
          });
        }
        if (failure.kind === "malformed") {
          return route.fulfill({
            contentType: "application/json",
            body: successEnvelope({
              accessToken: "only-one-token",
              accessTokenExpiry: 3_600_000,
            }),
          });
        }
        return route.fulfill({
          contentType: "application/json",
          body: successEnvelope({
            accessToken: "malformed-access",
            refreshToken: "malformed-refresh",
            accessTokenExpiry: 3_600_000,
          }),
        });
      });

      // When
      await page.goto("/login/callback?code=fixture-code&state=fixture-state");

      // Then
      await expect(
        page.getByRole("alert").filter({ hasText: "다시 시도" }),
      ).toBeVisible();
      await expect(
        page.getByRole("link", { name: "로그인 다시 시도하기" }),
      ).toBeVisible();
      await expect(page).toHaveURL(/\/login\/callback$/u);
      expect(oauth.callbackRequestCount()).toBe(1);
      expect(await page.context().cookies()).toEqual([]);
      await page.screenshot({
        path: testInfo.outputPath(`mobile-${failure.kind}.png`),
        fullPage: true,
      });
    });
  }
});
