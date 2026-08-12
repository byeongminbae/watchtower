import { expect, test } from "../fixtures";

const CALLBACK_PATH = "/bff/auth/naver/callback";
const TIMESTAMP = "2026-08-11T05:50:00";

test("Given an OAuth callback, when the frontend calls the API, then the browser requests only the Next BFF path", async ({
  page,
  oauth,
}) => {
  // Given
  await oauth.storeTransaction(page, "boundary-state");
  await page.route(`**${CALLBACK_PATH}?**`, (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        success: true,
        data: oauth.validTokens(),
        timestamp: TIMESTAMP,
      }),
    }),
  );
  const callbackRequest = page.waitForRequest((request) =>
    new URL(request.url()).pathname.endsWith(CALLBACK_PATH),
  );

  // When
  await page.goto(
    "/login/callback?code=boundary-code&state=boundary-state",
  );

  // Then
  const browserRequestUrl = new URL((await callbackRequest).url());
  expect(browserRequestUrl.origin).toBe("http://127.0.0.1:3000");
  expect(browserRequestUrl.pathname).toBe(CALLBACK_PATH);
  expect(browserRequestUrl.pathname).not.toContain("/api/v1/");
});
