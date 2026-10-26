import { expect, test } from "../fixtures";

test("브라우저의 API 요청은 BFF만 사용하고 서버는 현재 세션을 확인할 것이다", async ({ page, login }) => {
  // given
  await login();
  const apiRequests: string[] = [];
  page.on("request", request => { if (["fetch", "xhr"].includes(request.resourceType())) apiRequests.push(request.url()); });
  // when
  await page.goto("/watches");
  await expect(page.getByRole("heading", { name: "채용 공고" })).toBeVisible();
  // then
  expect(apiRequests.some(url => url.includes("/bff/auth/session"))).toBe(true);
  expect(apiRequests.some(url => new URL(url).pathname.startsWith("/api/"))).toBe(false);
  expect(apiRequests.every(url => new URL(url).origin === "http://127.0.0.1:3000")).toBe(true);
});
test("일반 회원이 관리자 주소를 직접 요청해도 서버에서 차단할 것이다", async ({ page, login }) => {
  // given
  await login();
  // when
  await page.goto("/admin/members");
  // then
  await expect(page).toHaveURL(/\/watches$/);
  await expect(page.getByRole("heading", { name: "회원 관리", exact: true })).toHaveCount(0);
});
test("JWT 쿠키를 위조해도 관리자 페이지에 접근할 수 없을 것이다", async ({ page, context }) => {
  // given
  await context.addCookies([{ name: "watchtower_jwt", value: "forged-admin", url: "http://127.0.0.1:3000" }]);
  // when
  await page.goto("/admin");
  // then
  await expect(page).toHaveURL(/\/login\?next=%2Fadmin$/);
  await expect(page.getByRole("button", { name: "네이버로 시작하기" })).toBeVisible();
});
