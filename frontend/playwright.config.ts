import { defineConfig, devices } from "@playwright/test";

const baseURL = "http://127.0.0.1:3000";
export default defineConfig({
  testDir: "./tests",
  testMatch: "**/*.spec.ts",
  outputDir: "test-results",
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: 2,
  reporter: [["list"], ["html", { open: "never" }]],
  use: { baseURL, storageState: { cookies: [], origins: [] }, trace: "retain-on-failure", screenshot: "only-on-failure" },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    { command: "node --experimental-strip-types tests/support/backend.ts", url: "http://127.0.0.1:4100/health", reuseExistingServer: false },
    { command: "npm run build && npm run start -- --hostname 127.0.0.1 --port 3000", url: baseURL,
      env: { INTERNAL_API_BASE_URL: "http://127.0.0.1:4100", NEXT_PUBLIC_API_LOG: "false" },
      reuseExistingServer: false, timeout: 120_000 }
  ]
});
