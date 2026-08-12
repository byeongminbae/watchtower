import { expect, test } from "./index";

test("Given the frontend server, when a browser opens the root route, then the page responds", async ({ page }) => {
  await page.goto("/");

  await expect(page).toHaveURL(/127\.0\.0\.1:3000/);
});
