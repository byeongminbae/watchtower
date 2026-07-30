import { expect, test } from "../fixtures";

const TIMESTAMP = "2026-07-29T10:00:00";
const PROFILE_IMAGE_URL = "https://example.com/profile.svg";

function successEnvelope(data: unknown): string {
  return JSON.stringify({ success: true, data, timestamp: TIMESTAMP });
}

function emptySuccessEnvelope(): string {
  return JSON.stringify({ success: true, timestamp: TIMESTAMP });
}

test.describe("MyPage member profile", () => {
  test("Given a valid member session, when MyPage opens, then current profile and edit controls work at every viewport", async ({
    page,
    oauth,
  }, testInfo) => {
    // Given
    const tokens = oauth.validTokens(7);
    let currentTokens = tokens;
    let renewRequestCount = 0;
    let logoutRequestCount = 0;
    await page.goto("/login");
    await page.evaluate(({ accessToken, refreshToken }) => {
      document.cookie = `watchtower_jwt=${accessToken}; Path=/; SameSite=Lax`;
      document.cookie = `watchtower_refresh=${refreshToken}; Path=/; SameSite=Lax`;
    }, tokens);
    await page.route("**/api/v1/member/7", (route) =>
      route.fulfill({
        contentType: "application/json",
        body: successEnvelope({
          id: 7,
          nickname: "실제 감시자",
          email: "watcher@example.com",
          profileImageUrl: PROFILE_IMAGE_URL,
          lastSignInAt: TIMESTAMP,
        }),
      }),
    );
    await page.route("**/api/v1/auth/renew?**", (route) => {
      renewRequestCount += 1;
      expect(route.request().method()).toBe("POST");
      expect(route.request().headers().authorization).toBe(
        `Bearer ${currentTokens.accessToken}`,
      );
      expect(
        new URL(route.request().url()).searchParams.get("refreshToken"),
      ).toBe(currentTokens.refreshToken);
      currentTokens = oauth.validTokens(7);
      return route.fulfill({
        contentType: "application/json",
        body: successEnvelope(currentTokens),
      });
    });
    await page.route("**/api/v1/auth/logout", (route) => {
      logoutRequestCount += 1;
      expect(route.request().method()).toBe("DELETE");
      expect(route.request().headers().authorization).toContain("Bearer ");
      return route.fulfill({
        contentType: "application/json",
        body: emptySuccessEnvelope(),
      });
    });
    await page.route(PROFILE_IMAGE_URL, (route) =>
      route.fulfill({
        contentType: "image/svg+xml",
        body: '<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72"><rect width="72" height="72" fill="#16a34a"/></svg>',
      }),
    );

    // When
    await page.goto("/mypage");

    // Then
    await expect(page.getByRole("heading", { name: "마이페이지" })).toBeVisible();
    await expect(page.getByRole("textbox", { name: "닉네임" })).toHaveValue("실제 감시자");
    const profileImages = page.getByRole("img", {
      name: "실제 감시자 프로필 사진",
    });
    await expect(profileImages).toHaveCount(2);
    await expect(profileImages.first()).toHaveAttribute("src", PROFILE_IMAGE_URL);
    await expect(
      page.getByText("백엔드 기능이 아직 제공되지 않습니다."),
    ).not.toBeVisible();
    for (const viewport of [
      { name: "mobile", width: 375, height: 812 },
      { name: "tablet", width: 768, height: 900 },
      { name: "desktop", width: 1280, height: 900 },
    ]) {
      await page.setViewportSize({
        width: viewport.width,
        height: viewport.height,
      });
      await page.screenshot({
        path: testInfo.outputPath(`mypage-view-${viewport.name}.png`),
      });
      await page.getByRole("button", { name: "세션 갱신" }).click();
      const renewalAlert = page.getByRole("alert").filter({
        hasText: "세션이 갱신되었습니다.",
      });
      await expect(renewalAlert).toBeVisible();
      await renewalAlert.evaluate(async (element) => {
        await Promise.all(
          element
            .getAnimations({ subtree: true })
            .map((animation) => animation.finished),
        );
      });
      await page.screenshot({
        path: testInfo.outputPath(`mypage-renew-success-${viewport.name}.png`),
      });
      await page.getByRole("button", { name: "Close" }).click();
      await expect(page.getByText("세션이 갱신되었습니다.")).not.toBeVisible();
    }
    expect(renewRequestCount).toBe(3);

    await page.getByRole("button", { name: "수정" }).click();
    await expect(page.getByRole("textbox", { name: "닉네임" })).toBeEnabled();
    await expect(page.getByRole("button", { name: "저장" })).toBeVisible();
    await expect(page.getByRole("button", { name: "취소" })).toBeVisible();
    await page.evaluate(
      () =>
        new Promise<void>((resolve) => {
          requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
        }),
    );
    await page.screenshot({
      path: testInfo.outputPath("mypage-edit-desktop-profile.png"),
      animations: "disabled",
    });
    await page.screenshot({
      path: testInfo.outputPath("mypage-edit-desktop-profile.jpg"),
      animations: "disabled",
      quality: 95,
    });

    await page.reload();
    await expect(page).toHaveURL(/\/mypage$/u);
    await expect(page.getByRole("textbox", { name: "닉네임" })).toHaveValue("실제 감시자");

    await page.getByRole("button", { name: "실제 감시자 메뉴" }).click();
    await page.getByRole("menuitem", { name: "로그아웃" }).click();
    await expect(page).toHaveURL(/\/$/u);
    expect(logoutRequestCount).toBe(1);
    expect(
      (await page.context().cookies()).filter((cookie) =>
        cookie.name.startsWith("watchtower_"),
      ),
    ).toEqual([]);
  });
});
