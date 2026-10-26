import type { Page } from "@playwright/test";
import { expect, test } from "../fixtures";

const browserErrors = new WeakMap<Page, string[]>();

test.beforeEach(({ page }) => {
  const errors: string[] = [];
  browserErrors.set(page, errors);
  page.on("pageerror", error => errors.push(error.message));
  page.on("console", message => { if (message.type() === "error") errors.push(message.text()); });
});

test.afterEach(({ page }) => {
  expect(browserErrors.get(page), "브라우저 실행 중 오류가 없어야 해요.").toEqual([]);
});

for (const role of ["NORMAL", "ADMIN"] as const) {
  test(`${role} 화면은 모바일과 데스크톱에서 가로 넘침 없이 표시될 것이다`, async ({ page, login }, info) => {
    // given
    await login(role);
    const routes = role === "NORMAL" ? ["/", "/watches", "/watches/9", "/mypage", "/mypage/notifications"] : ["/admin", "/admin/members", "/admin/watches"];
    // when
    for (const width of [375, 768, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      for (const route of routes) {
        await page.goto(route);
        if (route !== "/") await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
        await expect(page.getByRole("status")).toHaveCount(0);
        // then
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
        expect(overflow, `${route} at ${width}`).toBe(false);
        await page.screenshot({ path: info.outputPath(`${route.replaceAll("/", "-") || "home"}-${width}.png`), fullPage: true });
      }
    }
  });
}

test("비로그인 홈과 로그인 화면은 모바일과 태블릿과 데스크톱에서 가로 넘침 없이 표시될 것이다", async ({ page }, info) => {
  // given
  const routes = ["/", "/login"];
  // when
  for (const width of [375, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    for (const route of routes) {
      await page.goto(route);
      await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
      if (route === "/") await expect(page.getByRole("link", { name: "지금 감시 시작하기" })).toHaveAttribute("href", "/login");
      else await expect(page.getByRole("button", { name: "네이버로 시작하기" })).toBeVisible();
      // then
      await expect(page.getByRole("status")).toHaveCount(0);
      expect(await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth), `${route} at ${width}`).toBe(false);
      await page.screenshot({ path: info.outputPath(`guest-${route === "/" ? "home" : "login"}-${width}.png`), fullPage: true });
    }
  }
});

test("키보드로 본문 이동 후 계정 메뉴를 열고 닫으면 실행 버튼으로 초점이 돌아올 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/");
  const account = page.getByRole("button", { name: "테스트 회원 메뉴" });
  await expect(account).toBeVisible();
  // when
  await page.keyboard.press("Tab");
  // then
  await expect(page.getByRole("link", { name: "본문으로 이동" })).toBeFocused();
  // when
  await page.keyboard.press("Enter");
  // then
  await expect(page.locator("#main-content")).toBeFocused();
  // when
  await page.keyboard.press("Shift+Tab");
  // then
  await expect(account).toBeFocused();
  // when
  await page.keyboard.press("Enter");
  // then
  await expect(page.getByRole("menu")).toBeVisible();
  await expect(page.getByRole("menuitem", { name: "마이페이지", exact: true })).toBeFocused();
  // when
  await page.keyboard.press("Escape");
  // then
  await expect(page.getByRole("menu")).toHaveCount(0);
  await expect(account).toBeFocused();
  await expect(account).toHaveAttribute("aria-expanded", "false");
});

test("모바일 타워 등록 창은 필드와 등록 동작을 가로 넘침 없이 사용할 수 있을 것이다", async ({ page, login }, info) => {
  // given
  await login();
  await page.setViewportSize({ width: 375, height: 900 });
  await page.goto("/watches");
  const trigger = page.getByRole("button", { name: "타워 등록", exact: true });
  // when
  await trigger.click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("사이트 이름").fill("모바일에서 확인할 공지");
  await dialog.getByLabel("감시할 URL").fill("https://example.com/mobile");
  await expect(dialog.getByRole("status")).toHaveCount(0);
  // then
  await expect(dialog.getByLabel("사이트 이름")).toBeInViewport();
  await expect(dialog.getByLabel("감시할 URL")).toBeInViewport();
  const bounds = await dialog.evaluate(element => {
    const rect = element.getBoundingClientRect();
    return { left: rect.left, right: rect.right, overflow: element.scrollWidth > element.clientWidth };
  });
  expect(bounds.left).toBeGreaterThanOrEqual(0);
  expect(bounds.right).toBeLessThanOrEqual(375);
  expect(bounds.overflow).toBe(false);
  await page.screenshot({ path: info.outputPath("create-tower-mobile-fields.png") });
  // when
  const submit = dialog.getByRole("button", { name: "등록", exact: true });
  await submit.scrollIntoViewIfNeeded();
  // then
  await expect(submit).toBeEnabled();
  await expect(submit).toBeInViewport();
  await page.screenshot({ path: info.outputPath("create-tower-mobile-actions.png") });
  // when
  await dialog.getByRole("button", { name: "취소", exact: true }).click();
  // then
  await expect(dialog).toHaveCount(0);
  await expect(trigger).toBeFocused();
});

test("기본 shadcn 테마는 참고 페이지의 흑백 토큰과 기본 버튼 규격을 사용할 것이다", async ({ page, login }) => {
  // given
  await login();
  await page.goto("/watches");
  const button = page.getByRole("button", { name: "타워 등록", exact: true });
  await expect(button).toBeVisible();
  // when
  const styles = await button.evaluate(element => {
    const root = getComputedStyle(document.documentElement);
    const style = getComputedStyle(element);
    function pixels(color: string) {
      const canvas = document.createElement("canvas");
      canvas.width = canvas.height = 1;
      const context = canvas.getContext("2d");
      if (!context) throw new Error("색상 비교를 위한 캔버스를 만들 수 없어요.");
      context.fillStyle = color;
      context.fillRect(0, 0, 1, 1);
      return Array.from(context.getImageData(0, 0, 1, 1).data);
    }
    const reference = { primary: "oklch(0.205 0 0)", background: "oklch(1 0 0)", secondary: "oklch(0.97 0 0)", border: "oklch(0.922 0 0)" };
    return {
      actual: Object.fromEntries(Object.keys(reference).map(name => [name, pixels(root.getPropertyValue(`--${name}`).trim())])),
      reference: Object.fromEntries(Object.entries(reference).map(([name, color]) => [name, pixels(color)])),
      height: style.height,
      weight: style.fontWeight,
      font: style.fontFamily,
      fontLoaded: document.fonts.check('500 14px "Inter Variable"')
    };
  });
  // then
  expect(styles.actual).toEqual(styles.reference);
  expect(styles.height).toBe("32px");
  expect(styles.weight).toBe("500");
  expect(styles.font).toContain("Inter");
  expect(styles.fontLoaded).toBe(true);
});
