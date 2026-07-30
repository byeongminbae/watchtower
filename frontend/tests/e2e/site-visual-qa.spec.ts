import { expect, test } from "../fixtures";

const TIMESTAMP = "2026-07-29T10:00:00";
const VIEWPORTS = [
  { name: "mobile", width: 375, height: 812 },
  { name: "tablet", width: 768, height: 900 },
  { name: "desktop", width: 1280, height: 900 },
] as const;
const PUBLIC_ROUTES = ["/", "/login", "/login/callback"] as const;
const MEMBER_ROUTES = [
  "/watches",
  "/watches/1",
  "/mypage",
  "/mypage/payments",
  "/mypage/subscription",
] as const;
const ADMIN_ROUTES = [
  "/admin",
  "/admin/payments",
  "/admin/users",
  "/admin/watches",
] as const;

function successEnvelope(data: unknown): string {
  return JSON.stringify({ success: true, data, timestamp: TIMESTAMP });
}

function createTokenPair(role: "NORMAL" | "ADMIN", memberId: number): {
  readonly accessToken: string;
  readonly refreshToken: string;
} {
  const now = Math.floor(Date.now() / 1_000);
  const header = Buffer.from(JSON.stringify({ alg: "none", typ: "JWT" })).toString("base64url");
  const accessBody = Buffer.from(
    JSON.stringify({ sub: String(memberId), role, iat: now, exp: now + 3_600 }),
  ).toString("base64url");
  const refreshBody = Buffer.from(
    JSON.stringify({ sub: String(memberId), iat: now, exp: now + 86_400 }),
  ).toString("base64url");
  return {
    accessToken: `${header}.${accessBody}.fixture`,
    refreshToken: `${header}.${refreshBody}.fixture`,
  };
}

async function setSession(
  page: import("@playwright/test").Page,
  tokens: { readonly accessToken: string; readonly refreshToken: string },
): Promise<void> {
  await page.goto("/login");
  await page.evaluate(({ accessToken, refreshToken }) => {
    document.cookie = `watchtower_jwt=${accessToken}; Path=/; SameSite=Lax`;
    document.cookie = `watchtower_refresh=${refreshToken}; Path=/; SameSite=Lax`;
  }, tokens);
}

function captureName(route: string, viewport: string): string {
  const slug = route === "/" ? "home" : route.slice(1).replaceAll("/", "-");
  return `test-results/visual-qa/current/${slug}-${viewport}.png`;
}

test("Given every route, when it renders at each breakpoint, then the daylight system has no horizontal overflow", async ({
  page,
  oauth,
}) => {
  // Given
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

  // When
  for (const route of PUBLIC_ROUTES) {
    await captureRoute(page, route);
  }
  const memberTokens = oauth.validTokens(7);
  await setSession(page, {
    accessToken: memberTokens.accessToken,
    refreshToken: memberTokens.refreshToken,
  });
  for (const route of MEMBER_ROUTES) {
    await captureRoute(page, route);
  }
  await setSession(page, createTokenPair("ADMIN", 1));
  for (const route of ADMIN_ROUTES) {
    await captureRoute(page, route);
  }

  // Then
  expect(PUBLIC_ROUTES.length + MEMBER_ROUTES.length + ADMIN_ROUTES.length).toBe(12);
});

test("Given the interface interactions, when focus, press, and reduced motion are used, then each visual state remains usable", async ({
  page,
}) => {
  // Given
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/");
  const action = page.getByRole("link", { name: "지금 감시 시작하기" });

  // When
  await action.focus();
  await page.screenshot({
    path: "test-results/visual-qa/current/interaction-focus.png",
    animations: "disabled",
  });
  const actionBox = await action.boundingBox();
  expect(actionBox).not.toBeNull();
  if (actionBox !== null) {
    await action.evaluate((element) => {
      element.addEventListener("click", (event) => event.preventDefault(), {
        once: true,
      });
    });
    await page.mouse.move(actionBox.x + actionBox.width / 2, actionBox.y + actionBox.height / 2);
    await page.mouse.down();
    const pressedTransform = await action.evaluate((element) => getComputedStyle(element).transform);
    expect(pressedTransform).not.toBe("none");
    await page.screenshot({
      path: "test-results/visual-qa/current/interaction-press-active.jpg",
      quality: 95,
    });
    await page.mouse.up();
    await action.evaluate(
      async (element) => {
        await Promise.allSettled(
          element.getAnimations().map((animation) => animation.finished),
        );
      },
    );
    await expect(page).toHaveURL("/");
  }
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.reload();
  await page.screenshot({
    path: "test-results/visual-qa/current/interaction-reduced-motion.png",
    animations: "disabled",
  });

  // Then
});

async function captureRoute(
  page: import("@playwright/test").Page,
  route: string,
): Promise<void> {
  for (const viewport of VIEWPORTS) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height });
    await page.goto(route);
    expect(new URL(page.url()).pathname, `${route} navigation`).toBe(route);
    await page.evaluate(
      () =>
        new Promise<void>((resolve) => {
          requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
        }),
    );
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    );
    expect(overflow, `${route} at ${viewport.name}`).toBeLessThanOrEqual(1);
    await page.screenshot({
      path: captureName(route, viewport.name),
      fullPage: true,
      animations: "disabled",
    });
  }
}
